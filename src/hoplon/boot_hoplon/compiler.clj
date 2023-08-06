;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^:no-doc hoplon.boot-hoplon.compiler
  (:require
    cljs.util
    [clojure.pprint             :as pp]
    [clojure.java.io            :as io]
    [clojure.string             :as string]
    [clojure.set                :as set]
    [hoplon.boot-hoplon.tagsoup :as tags]
    [hoplon.boot-hoplon.refer   :as refer])
  (:import
    [java.util UUID]
    [clojure.lang LineNumberingPushbackReader]
    [java.io StringReader]))

(def ^:dynamic *printer* prn)

(defmacro cache-key []
  (or (System/getProperty "hoplon.cacheKey")
      (let [u (.. (UUID/randomUUID) toString (replaceAll "-" ""))]
        (System/setProperty "hoplon.cacheKey" u)
        u)))

(defn bust-cache
  [path]
  (let [[f & more] (reverse (string/split path #"/"))
        [f1 f2]    (string/split f #"\." 2)]
    (->> [(str f1 "." (cache-key)) f2]
         (string/join ".")
         (conj more)
         (reverse)
         (string/join "/"))))

(defn munge-page-name [x]
  (-> (str "_" (name x)) (string/replace #"\." "_DOT_") munge))

(defn munge-page [ns]
  (let [ap "hoplon.app-pages."]
    (if (symbol? ns) ns (symbol (str ap (munge-page-name ns))))))

(defn read-string-1
  [x]
  (when x
    (with-open [r (LineNumberingPushbackReader. (StringReader. x))]
      [(read r false nil)
       (string/join (concat (repeat (dec (.getLineNumber r)) "\n") [(slurp r)]))])))

(defn up-parents [path name]
  (let [[_f & dirs] (string/split path #"/")]
    (->> [name] (concat (repeat (count dirs) "../")) (apply str))))

(defn inline-code [s process]
  (let [lines (string/split s #"\n")
        start #";;\{\{\s*$"
        end #"^\s*;;\}\}\s*$"
        pad #"^\s*"
        unpad #(string/replace %1 (re-pattern (format "^\\s{0,%d}" %2)) "")]
    (loop [txt nil, i 0, [line & lines] lines, out []]
      (if-not line
        (string/join "\n" out)
        (if-not txt
          (if (re-find start line)
            (recur [] i lines out)
            (recur txt i lines (conj out line)))
          (if (re-find end line)
            (let [s (process (string/trim (string/join "\n" txt)))]
              (recur nil 0 (rest lines) (conj (pop out) (str (peek out) s (first lines)))))
            (let [i (if-not (empty? txt) i (count (re-find pad line)))]
              (recur (conj txt (unpad line i)) i lines out))))))))

(defn ->cljs-str [s]
  (if (not= \< (first (string/trim s)))
    s
    (tags/parse-string (inline-code s tags/html-escape))))

(defn output-path [forms] (-> forms first second str))
(defn output-path-for [path] (-> path slurp ->cljs-str output-path))

(defn make-nsdecl [[_ ns-sym & forms] {:keys [refers]}]
  (let [ns-sym    (symbol ns-sym)
        core-ns   '#{hoplon.core javelin.core}
        refers    (if-not refers (conj core-ns 'hoplon.jquery) (set/union core-ns refers))
        rm?       #{} ;;#(or (contains? refers %) (and (seq %) (contains? refers (first %))))
        mk-req    #(remove nil? (concat (remove rm? %2) (map %1 refers (repeat %3))))
        clauses   (->> (tree-seq seq? seq forms) (filter seq?) (group-by first))
        exclude   (when-let [e (:refer-hoplon clauses)] (nth (first e) 2))
        combine   #(mapcat (partial drop 1) (% clauses))
        req       (combine :require)
        reqm      (combine :require-macros)
        reqs      `(:require ~@(mk-req refer/make-require req exclude))
        macros    `(:require-macros ~@(mk-req refer/make-require-macros reqm exclude))
        other?    #(-> #{:require :require-macros :refer-hoplon}
                       ((comp not contains?) (first %)))
        others    (->> forms (filter list?) (filter other?))]
    `(~'ns ~ns-sym ~@others ~reqs ~macros)))

(defn forms-str [ns-form body]
  (str (binding [*print-meta* true] (pr-str ns-form)) body))

(defn ns->path [ns]
  (-> ns munge (string/replace \. \/) (str ".cljs")))

(defn html-str [js-uri]
  (tags/print-page
    "html"
    `(~'html {}
             (~'head {} (~'meta {:charset "utf-8"}))
             (~'body {} (~'script {:type "text/javascript"
                                   :src ~(str js-uri)})))))

(defn compile-forms [nsdecl body {:keys [bust-cache _refers] :as opts}]
  (case (first nsdecl)
    ns   {:cljs (forms-str (make-nsdecl nsdecl opts) body) :ns (second nsdecl)}
    page (let [[_ page & _] nsdecl
               outpath     (output-path [nsdecl])
               page-ns     (munge-page page)
               cljsstr     (let [[h _ & t] (make-nsdecl nsdecl opts)]
                             (forms-str (list* h page-ns t) body))
               js-out      (if-not bust-cache outpath (hoplon.boot-hoplon.compiler/bust-cache outpath))
               js-uri      (-> js-out (string/split #"/") last (str ".js"))
               htmlstr     (html-str js-uri)
               ednstr      (pr-str {:require  [(symbol page-ns)]})]
           {:html htmlstr :edn ednstr :cljs cljsstr :ns page-ns :file outpath :js-file js-out})))

(defn pp [form] (pp/write form :dispatch pp/code-dispatch))

(defn- write [f s]
  (when (and f s)
    (doto f io/make-parents (spit s))))

(defn compile-string
  [say-it forms-as-str path cljsdir htmldir & {{:keys [bust-cache _refers] :as opts} :opts}]
  (let [[[_tag ns-sym & clauses :as ns-form] body] (read-string-1 (->cljs-str forms-as-str))
        {html-path :hoplon/page} (meta ns-sym)]
    (cond (.endsWith path ".cljs")
          (let [gen-html? #(= :page (first %))
                html-cls  (->> clauses (filter gen-html?) first)
                html-path (or html-path (second html-cls))]
            (when html-path
              (let [outpath   (if-not bust-cache html-path (hoplon.boot-hoplon.compiler/bust-cache html-path))
                    js-uri    (-> outpath (string/split #"/") last (str ".js"))
                    edn-path  (str outpath ".cljs.edn")]
                (say-it outpath)
                (write (io/file htmldir html-path) (html-str js-uri))
                (write (io/file cljsdir edn-path) (pr-str {:require [ns-sym]})))))
          (.endsWith path ".hl")
          (let [{:keys [cljs ns html edn file js-file]} (compile-forms ns-form body opts)
                cljs-out (io/file cljsdir (ns->path ns))]
            (write cljs-out cljs)
            (when file
              (let [html-out (io/file htmldir file)
                    edn-out  (io/file cljsdir (str js-file ".cljs.edn"))]
                (say-it file)
                (write edn-out edn)
                (write html-out html)))))))

(defn compile-file [say-it path & args]
  (apply compile-string say-it (slurp (io/resource path)) path args))
