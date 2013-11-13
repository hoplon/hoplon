;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.compiler
  (:require
    [clojure.pprint                         :as pp]
    [clojure.java.io                        :as io]
    [clojure.string                         :as str]
    [cljs.compiler                          :as cljs]
    [tailrecursion.hoplon                   :as hl]
    [tailrecursion.hoplon.compiler.tagsoup  :as tags]
    [tailrecursion.hoplon.compiler.refer    :as refer]))

(def ^:dynamic *printer* prn)

(def html-tags
  '[a abbr acronym address applet area article
    aside audio b base basefont bdi bdo big blockquote body br
    button canvas caption center cite code col colgroup command
    data datalist dd del details dfn dir div dl dt em embed
    eventsource fieldset figcaption figure font footer form frame
    frameset h1 h2 h3 h4 h5 h6 head header hgroup hr html i
    iframe img input ins isindex kbd keygen label legend li link
    html-map mark menu html-meta meter nav noframes noscript object
    ol optgroup option output p param pre progress q rp rt ruby
    s samp script section select small source span strike strong
    style sub summary sup table tbody td textarea tfoot th thead
    html-time title tr track tt u ul html-var video wbr $text
    $comment spliced])

(defn up-parents [path name]
  (let [[f & dirs] (str/split path #"/")]
    (->> [name] (concat (repeat (count dirs) "../")) (apply str))))

(defn munge-path [path]
  (-> (str "__" path)
    (str/replace "_" "__")
    (str/replace "/" "_")
    (str/replace ":" "__COLON__")))

(defn inline-code [s process]
  (let [lines (str/split s #"\n")
        start #";;\{\{\s*$"
        end   #"^\s*;;\}\}\s*$"
        pad   #"^\s*"
        unpad #(str/replace %1 (re-pattern (format "^\\s{0,%d}" %2)) "")]
    (loop [txt nil, i 0, [line & lines] lines, out []]
      (if-not line
        (str/join "\n" out) 
        (if-not txt
          (if (re-find start line)
            (recur [] i lines out)
            (recur txt i lines (conj out line)))
          (if (re-find end line)
            (let [s (process (str/trim (str/join "\n" txt)))]
              (recur nil 0 (rest lines) (conj (pop out) (str (peek out) s (first lines)))))
            (let [i (if-not (empty? txt) i (count (re-find pad line)))]
              (recur (conj txt (unpad line i)) i lines out))))))))

(defn as-forms [s]
  (if (= \< (first (str/trim s))) 
    (tags/parse-string (inline-code s tags/html-escape))
    (read-string (str "(" (inline-code s pr-str) ")"))))

(defn output-path     [forms] (-> forms first second str))
(defn output-path-for [path]  (-> path slurp as-forms output-path))

(defn make-nsdecl
  [[_ ns-sym & forms]]
  (let [ns-sym  (symbol ns-sym)
        ns-syms '#{tailrecursion.hoplon tailrecursion.javelin}
        rm?     #(or (contains? ns-syms %) (and (seq %) (contains? ns-syms (first %))))
        mk-req  #(concat (remove rm? %2) (map %1 ns-syms (repeat %3)))
        clauses (->> (tree-seq list? seq forms) (filter list?) (group-by first))
        exclude (when-let [e (:refer-hoplon clauses)] (nth (first e) 2))
        combine #(mapcat (partial drop 1) (% clauses))
        req     (combine :require)
        reqm    (combine :require-macros)
        reqs    `(:require ~@(mk-req refer/make-require req exclude))
        macros  `(:require-macros ~@(mk-req refer/make-require-macros reqm exclude))
        other?  #(-> #{:require :require-macros :refer-hoplon}
                   ((comp not contains?) (first %)))
        others  (->> forms (filter list?) (filter other?))]
    `(~'ns ~ns-sym ~@others ~reqs ~macros)))

(defn forms-str [forms]
  (str/join "\n" (map #(with-out-str (*printer* %)) forms)))

(defn compile-lib [[[ns* & _ :as nsdecl] & tlfs]]
  (when (= 'ns ns*) (forms-str (cons (make-nsdecl nsdecl) tlfs))))

(defn compile-forms [forms js-file]
  (let [[nsdecl & tlfs] forms]
    (if (= 'ns (first nsdecl))
      {:cljs (forms-str (cons (make-nsdecl nsdecl) tlfs))}
      (let [[[_ & setup] html] ((juxt butlast last) forms)
            outpath   (output-path forms)
            js-uri    (up-parents outpath (.getName js-file))
            mkns      #(symbol (str "tailrecursion.hoplon.app-pages." (gensym)))
            nsdecl    (let [[h n & t] (make-nsdecl nsdecl)] (list* h (mkns) t))
            nsname    (cljs/munge (second nsdecl)) 
            [_ htmlattr [head body]]  (hl/parse-e html)
            [_ headattr heads]        (hl/parse-e head)
            [_ bodyattr bodies]       (hl/parse-e body)
            heads     (map #(let [[t a c] (hl/parse-e %)] (list* t (or a {}) c)) heads)
            s-nodep   (list 'script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true;")
            s-main    (list 'script {:type "text/javascript" :src js-uri})
            s-init    (list 'script {:type "text/javascript"} (str nsname ".hoploninit();"))
            s-html    (list 'html (or htmlattr {})
                            (list* 'head (or headattr {}) heads)
                            (list 'body (or bodyattr {}) s-nodep s-main s-init))
            htmlstr   (tags/print-page "html" s-html)
            cljs      (list nsdecl
                            (list 'defn (symbol "^:export") 'hoploninit []
                                  (cons 'do setup) 
                                  (list (symbol "tailrecursion.hoplon/init") (vec bodies))))
            cljsstr   (forms-str cljs)]
        {:html htmlstr :cljs cljsstr :file outpath}))))

(defn pp [form] (pp/write form :dispatch pp/code-dispatch))

(defn compile-string
  [forms-str path js-file cljsdir htmldir & {:keys [opts]}]
  (when-let [forms (as-forms forms-str)]
    (binding [*printer* (if (:pretty-print opts) pp prn)]
      (let [compiled (compile-forms forms js-file)
            cljs-out (io/file cljsdir (str (munge-path path) ".cljs"))
            html-out (when-let [f (:file compiled)] (io/file htmldir f))
            write    #(when (and %1 %2) (spit (doto %1 io/make-parents) %2))]
        (write cljs-out (:cljs compiled))
        (write html-out (:html compiled))))))

(defn compile-file [f & args]
  (apply compile-string (slurp f) (.getPath f) args))
