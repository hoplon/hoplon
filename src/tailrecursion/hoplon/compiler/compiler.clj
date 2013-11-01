;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.compiler
  (:require
    [clojure.java.io                        :refer  [file resource make-parents]]
    [clojure.pprint                         :as     pprint]
    [clojure.string                         :as     string]
    [cljs.compiler                          :as     cljsc]
    [tailrecursion.javelin                  :refer  [make-require make-require-macros]]
    [tailrecursion.hoplon.compiler.tagsoup  :as     ts]))

(def ^:dynamic *printer* prn)
(def file->page (atom {}))

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
  (let [[f & dirs] (string/split path #"/")]
    (->> [name] (concat (repeat (count dirs) "../")) (apply str))))

(defn munge-path [path]
  (-> (str "__" path)
    (string/replace "_" "__")
    (string/replace "/" "_")
    (string/replace ":" "__COLON__")))

(defn as-forms [s]
  (case (first (string/trim s))
    \< (-> s ts/parse-string ts/tagsoup->hoplon)
    (try
    (read-string (str "(" s ")"))
      (catch Throwable e (println s))
      )))

(defn output-path     [forms] (-> forms first second str))
(defn output-path-for [path]  (-> path slurp as-forms output-path))

(defn make-nsdecl
  [[_ ns-sym & forms]]
  (let [ns-syms '#{tailrecursion.hoplon tailrecursion.javelin}
        rm?     #(or (contains? ns-syms %) (and (seq %) (contains? ns-syms (first %))))
        mk-req  #(concat (remove rm? %2) (map %1 ns-syms))
        clauses (->> (tree-seq list? seq forms) (filter list?) (group-by first))
        combine #(mapcat (partial drop 1) (% clauses))
        reqs    `(:require ~@(mk-req make-require (combine :require)))
        macros  `(:require-macros ~@(mk-req make-require-macros (combine :require-macros)))
        other?  #(not (contains? #{:require :require-macros} (first %)))
        others  (->> forms (filter list?) (filter other?))]
    `(~'ns ~ns-sym ~@others ~reqs ~macros)))

(defn forms-str [forms]
  (string/join "\n" (map #(with-out-str (*printer* %)) forms)))

(defn compile-lib [[[ns* & _ :as nsdecl] & tlfs]]
  (when (= 'ns ns*) (forms-str (cons (make-nsdecl nsdecl) tlfs))))

(defn compile-forms [forms js-file]
  (let [[nsdecl & tlfs] forms]
    (if (= 'ns (first nsdecl))
      {:cljs (forms-str (cons (make-nsdecl nsdecl) tlfs))}
      (let [[[_ & setup] html] ((juxt butlast last) forms)
            outpath   (output-path forms)
            js-uri    (up-parents outpath (.getName js-file))
            mkns      #(symbol (str "tailrecursion.hoplon.app-pages." (munge %)))
            nsdecl    (let [[h n & t] (make-nsdecl nsdecl)] (list* h (mkns n) t))
            nsname    (cljsc/munge (second nsdecl)) 
            s-nodep   (list 'script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true;")
            s-main    (list 'script {:type "text/javascript" :src js-uri})
            s-init    (list 'script {:type "text/javascript"} (str nsname ".hoploninit();"))
            s-html    (list 'html
                            (list 'head (list 'meta {:charset "utf-8"}))
                            (list 'body s-nodep s-main s-init))
            htmlstr   (ts/pp-forms "html" s-html)
            cljs      (list nsdecl
                            (list 'defn (symbol "^:export") 'hoploninit []
                                  (cons 'do setup) 
                                  (list (symbol "tailrecursion.hoplon/init") html)))
            cljsstr   (forms-str cljs)]
        {:html htmlstr :cljs cljsstr :file outpath}))))

(defn pp [form] (pprint/write form :dispatch pprint/code-dispatch))

(defn delete-path [p]
  (doseq [[k v] (get @file->page p)]
    (when (and v (.exists v)) (.delete v)))
  (swap! file->page dissoc p))

(defn compile-string
  [forms-str path js-file cljsdir htmldir & {:keys [opts]}]
  (when-let [forms (as-forms forms-str)]
    (binding [*printer* (if (:pretty-print opts) pp prn)]
      (let [compiled (compile-forms forms js-file)
            cljs-out (file cljsdir (str (munge-path path) ".cljs"))
            html-out (when-let [f (:file compiled)] (file htmldir f))
            write    #(when (and %1 %2) (spit (doto %1 make-parents) %2))]
        (delete-path path)
        (swap! file->page assoc path {:cljs cljs-out :html html-out})
        (write cljs-out (:cljs compiled))
        (write html-out (:html compiled))))))

(defn compile-file [f & args]
  (if (and (instance? java.io.File f) (not (.exists f)))
    (delete-path (.getPath f))
    (apply compile-string (slurp f) (.getPath f) args)))
