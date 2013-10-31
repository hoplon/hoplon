;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.compiler
  (:require
    [clojure.java.io                        :refer  [file make-parents]]
    [clojure.pprint                         :as     pprint]
    [clojure.string                         :as     string]
    [cljs.compiler                          :as     cljsc]
    [tailrecursion.javelin                  :refer  [make-require make-require-macros]]
    [tailrecursion.hoplon.compiler.tagsoup  :as     ts]))

(def ^:dynamic *current-file* nil)
(def ^:dynamic *printer*      prn)

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
  (-> (str "__" path) (string/replace "_" "__") (string/replace "/" "_")))

(defn as-forms-string [s type]
  (cond (= type "html") (-> s ts/parse-string ts/tagsoup->hoplon)
        (= type "cljs") (read-string (str "(" s ")"))))

(defn as-forms-path [path]
  (as-forms-string (slurp path) (last (re-find #"[^.]+\.([^.]+)\.hl$" path))))

(defn output-path     [forms] (-> forms first second str))
(defn output-path-for [path]  (-> path as-forms-path output-path))

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

(defn compile-forms [forms js-file]
  (let [[[nsdecl & setup] html] ((juxt butlast last) forms)
        outpath   (output-path forms)
        js-uri    (up-parents outpath (.getName js-file))
        nsdecl    (let [n (make-nsdecl nsdecl)]
                    (concat (take 1 n) [(munge (second n))] (drop 2 n))) 
        nsname    (cljsc/munge (second nsdecl)) 
        s-nodep   (list 'script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true;")
        s-main    (list 'script {:type "text/javascript" :src js-uri})
        s-init    (list 'script {:type "text/javascript"} (str nsname ".hoploninit();"))
        s-html    (list 'html
                        (list 'head (list 'meta {:charset "utf-8"}))
                        (list 'body s-nodep s-main s-init))
        htmlstr   (ts/pp-forms "html" s-html)
        cljs      (concat
                    (list nsdecl)
                    (list
                      (list 'defn (symbol "^:export") 'hoploninit []
                            (cons 'do setup) 
                            (list (symbol "tailrecursion.hoplon/init") html))))
        cljsstr (string/join "\n" (map #(with-out-str (*printer* %)) cljs))]
    {:html htmlstr :cljs cljsstr :file outpath}))

(defn pp [form] (pprint/write form :dispatch pprint/code-dispatch))

(defn compile-file
  [f js-file cljsdir htmldir & {:keys [opts]}]
  (when-let [forms (as-forms-path (.getPath f))]
    (binding [*current-file*  f
              *printer*       (if (:pretty-print opts) pp prn)]
      (let [compiled (compile-forms forms js-file)
            cljs-out (file cljsdir (str (munge-path (.getPath f)) ".cljs"))
            html-out (file htmldir (:file compiled))
            write    #(spit (doto %1 make-parents) %2)]
        (doall (map write [cljs-out html-out] ((juxt :cljs :html) compiled)))))))
