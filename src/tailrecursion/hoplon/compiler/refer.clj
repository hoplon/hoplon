;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.refer
  (:require
    [clojure.pprint  :as p]
    [cljs.analyzer   :as a]
    [clojure.string  :as s]
    [clojure.java.io :as io]
    [clojure.walk    :refer [prewalk]]
    [clojure.set     :refer [difference union]]))

;; util ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-let
  "Binds resource to binding and evaluates body.  Then, returns
  resource.  It's a cross between doto and with-open."
  [[binding resource] & body]
  `(let [~binding ~resource] ~@body ~binding))

;; mirroring ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn nsym->path [sym ext]
  (-> (str sym)
      (s/replace "." java.io.File/separator)
      (s/replace "-" "_")
      (str "." ext)))

(defn all-list-forms [forms]
  (filter list? (tree-seq coll? seq forms)))

(defn read-file [f]
  (with-open [in (java.io.PushbackReader. (io/reader f))]
    (->> (repeatedly #(read in false ::eof))
         (take-while (partial not= ::eof))
         doall)))

(defn ops-in [op-sym sym ext]
  (let [ns-file (io/resource (nsym->path sym ext))]
    (->>
     (read-file ns-file)
     list*
     (tree-seq coll? seq)
     (filter list?)
     (filter (comp (partial = op-sym) first))
     (mapv second))))

(defn mirrored-defs [ns-sym]
  (let [remote-defs (ops-in 'def ns-sym "cljs")]
    (map (fn [r] `(def ~r ~(symbol (str ns-sym) (str r)))) remote-defs)))

(defn mirrored-defns [ns-sym]
  (let [remote-defns (ops-in 'defn ns-sym "cljs")]
    (map (fn [r] `(defn ~r [& args#]
                    (apply ~(symbol (str ns-sym) (str r)) args#)))
         remote-defns)))

(defn mirror-def-all [ns-sym & {:keys [syms]}]
  (let [syms (distinct (into ['def 'defn 'defmulti] syms)) 
        defs (mapcat ops-in syms (repeat ns-sym) (repeat "cljs"))]
    (map (fn [r] `(def ~r ~(symbol (str ns-sym) (str r)))) defs)))

(defn exclude [ops exclusions]
  (vec (difference (set ops) (set exclusions))))

(defn make-require [ns-sym & [exclusions]]
  (let [syms ['def 'defn 'defmulti]
        ops  (mapcat ops-in syms (repeat ns-sym) (repeat "cljs"))]
    [ns-sym :refer (exclude ops exclusions)]))

(defn make-require-macros [ns-sym & [exclusions]]
  [ns-sym :refer (exclude (ops-in 'defmacro ns-sym "clj") exclusions)])

(defmacro mirror [ns-sym]
  `(do ~@(mirrored-defs  ns-sym)
       ~@(mirrored-defns ns-sym)))

(defmacro refer-all [ns-sym & more]
  `(do ~@(apply mirror-def-all ns-sym more)))
