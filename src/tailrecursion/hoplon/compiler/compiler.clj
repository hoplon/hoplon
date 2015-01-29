;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.compiler
  (:require
    [clojure.pprint :as pp]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [tailrecursion.hoplon.compiler.tagsoup :as tags]
    [tailrecursion.hoplon.compiler.util :as util]
    [tailrecursion.hoplon.compiler.refer :as refer]))

(def ^:dynamic *printer* prn)

(defn up-parents [path name]
  (let [[f & dirs] (str/split path #"/")]
    (->> [name] (concat (repeat (count dirs) "../")) (apply str))))

(defn inline-code [s process]
  (let [lines (str/split s #"\n")
        start #";;\{\{\s*$"
        end #"^\s*;;\}\}\s*$"
        pad #"^\s*"
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
    (util/read-string (inline-code s pr-str))))

(defn output-path [forms] (-> forms first second str))
(defn output-path-for [path] (-> path slurp as-forms output-path))

(defn make-nsdecl
  [[_ ns-sym & forms]]
  (let [ns-sym (symbol ns-sym)
        ns-syms '#{tailrecursion.hoplon tailrecursion.javelin}
        rm? #(or (contains? ns-syms %) (and (seq %) (contains? ns-syms (first %))))
        mk-req #(concat (remove rm? %2) (map %1 ns-syms (repeat %3)))
        clauses (->> (tree-seq list? seq forms) (filter list?) (group-by first))
        exclude (when-let [e (:refer-hoplon clauses)] (nth (first e) 2))
        combine #(mapcat (partial drop 1) (% clauses))
        req (combine :require)
        reqm (combine :require-macros)
        reqs `(:require ~@(mk-req refer/make-require req exclude))
        macros `(:require-macros ~@(mk-req refer/make-require-macros reqm exclude))
        other? #(-> #{:require :require-macros :refer-hoplon}
                    ((comp not contains?) (first %)))
        others (->> forms (filter list?) (filter other?))]
    `(~'ns ~ns-sym ~@others ~reqs ~macros)))

(defn forms-str [forms]
  (str/join "\n" (map #(with-out-str (*printer* %)) forms)))

(defn compile-lib [[[ns* & _ :as nsdecl] & tlfs]]
  (when (= 'ns ns*) (forms-str (cons (make-nsdecl nsdecl) tlfs))))

(defn ns->path [ns]
  (-> ns munge (str/replace \. \/) (str ".cljs")))

(defn compile-forms [forms]
  (require 'cljs.compiler)
  (let [[nsdecl & tlfs] forms]
    (if (= 'ns (first nsdecl))
      {:cljs (forms-str (cons (make-nsdecl nsdecl) tlfs)) :ns (second nsdecl)}
      (let [[_ page & _] nsdecl
            outpath (output-path forms)
            page-ns (util/munge-page page)
            nsdecl (let [[h _ & t] (make-nsdecl nsdecl)]
                     `(~h ~page-ns ~@t))
            cljs `(~nsdecl
                    (defn ~(symbol "^:export") ~'hoploninit []
                      ~@tlfs
                      (~'tailrecursion.hoplon/init)))
            cljsstr (forms-str cljs)]
        {:cljs cljsstr :ns page-ns :file outpath}))))

(defn pp [form] (pp/write form :dispatch pp/code-dispatch))

(defn- write [f s]
  (when (and f s)
    (doto f io/make-parents (spit s))))

(defn compile-string
  [forms-str cljsdir & {:keys [opts]}]
  (let [{:keys [pretty-print]} opts
        {:keys [cljs ns]}
        (when-let [forms (as-forms forms-str)]
          (binding [*printer* (if pretty-print pp prn)]
            (compile-forms forms)))
        cljs-out (io/file cljsdir (ns->path ns))]
    (write cljs-out cljs)))

(defn compile-file [f & args]
  (apply compile-string (slurp f) args))
