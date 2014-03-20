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

(defn munge-page [x]
  (-> (str "_" (name x)) (str/replace #"\." "_DOT_") munge))

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

(defn compile-forms [forms js-path]
  (require 'cljs.compiler)
  (let [[nsdecl & tlfs] forms
        cljs-munge (resolve 'cljs.compiler/munge)]
    (if (= 'ns (first nsdecl))
      {:cljs (forms-str (cons (make-nsdecl nsdecl) tlfs))}
      (let [[_ page & _] nsdecl
            outpath   (output-path forms)
            js-uri    (up-parents outpath js-path)
            app-pages "tailrecursion.hoplon.app-pages"
            page-ns   (symbol (str app-pages "." (munge-page page)))
            nsdecl    (let [[h n & t] (make-nsdecl nsdecl)]
                        `(~h ~page-ns ~@t))
            s-html    `(~'html {}
                         (~'head {}
                           (~'meta {:charset "utf-8"})
                           (~'script {:type "text/javascript" :src ~js-uri})
                           (~'script {:type "text/javascript"}
                             ~(str (cljs-munge (second nsdecl)) ".hoploninit();")))
                         (~'body {}))
            htmlstr   (tags/print-page "html" s-html)
            cljs      `(~nsdecl
                         (defn ~(symbol "^:export") ~'hoploninit []
                           ~@tlfs
                           (~'tailrecursion.hoplon/init)))
            cljsstr   (forms-str cljs)]
        {:html htmlstr :cljs cljsstr :file outpath}))))

(defn pp [form] (pp/write form :dispatch pp/code-dispatch))

(def cache (atom {}))

(defn compile-string
  [forms-str path js-path cljsdir htmldir & {:keys [opts]}]
  (let [{cache? :cache :keys [pretty-print]} opts
        cached      (get @cache path)
        cljs-out    (io/file cljsdir (str (munge-path path) ".cljs"))
        last-mod    (.lastModified (io/file path))
        use-cached? (<= last-mod (get cached :last-modified 0))
        write       (fn [f s m]
                      (when (and f s)
                        (spit (doto f io/make-parents) s)
                        (when m (.setLastModified f m))))]
    (let [{mod :last-modified :keys [file cljs html]}
          (if use-cached?
            cached
            (when-let [forms (as-forms forms-str)]
              (binding [*printer* (if pretty-print pp prn)]
                (let [compiled (-> (compile-forms forms js-path)
                                 (assoc :last-modified last-mod))]
                  (if (= cache? false)
                    compiled
                    (get (swap! cache assoc path compiled) path))))))]
      (write cljs-out cljs mod)
      (write (when file (io/file htmldir file)) html mod))))

(defn compile-file [f & args]
  (apply compile-string (slurp f) (.getPath f) args))
