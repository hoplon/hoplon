;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.tagsoup
  (:refer-clojure :exclude [replace])
  (:require
    [pl.danieljanus.tagsoup               :as ts]
    [clojure.string                       :as cs :refer [replace]]
    [tailrecursion.hoplon.compiler.pprint :as pp :refer [pprint]]
    [clojure.walk                         :as cw :refer [postwalk]]))

(def parse          ts/parse)
(def parse-string   ts/parse-string)
(def children       ts/children)
(def script?        #(and (vector? %) (= :script (first %))))
(def hoplon-script? #(and (script? %) (= "text/hoplon" (:type (second %)))))
(def cljs-attr?     #(and (string? %) (re-find #"^\s*\{\{.*\}\}\s*" %)))
(def walk-attr      #(if-not (cljs-attr? %) % (-> % (replace #"^\s*\{\{\s*" "")
                                                    (replace #"\s*\}\}\s*$" "")
                                                    read-string)))

(defn tagsoup->hoplon-1 [elem]
  (cond (string? elem)        (list (list '$text elem)) 
        (hoplon-script? elem) (read-string (str "(" (nth elem 2) ")"))
        :else
        (let [[t attrs & kids] elem
              tag   (symbol (name t)) 
              kids  (apply concat (map tagsoup->hoplon-1 kids)) 
              expr  (concat (list tag) (when (seq attrs) (list attrs)) kids)]
          (list (if (< 1 (count expr)) expr (first expr))))))

(defn tagsoup->hoplon [elem]
  (let [first* #(if (seq? %) (first %))
        last*  #(if (seq? %) (last %))
        rest*  #(drop-while (fn [x] (or (= 'html x) (map? x))) %)
        forms  (postwalk walk-attr (tagsoup->hoplon-1 elem))]
    (if (= 'html (first* (last* (first* forms)))) (rest* (first forms)) forms)))

(defn pedanticize
  [form]
  (cond (string? form) (list '$text form)
        (symbol? form) (list form {})
        (seq? form)
        (let [[tag & tail] form] 
          (if (or (= '$text tag) (= '$comment tag))
            form
            (let [attr (if (map? (first tail)) (first tail) {}) 
                  kids (map pedanticize (if (map? (first tail)) (rest tail) tail))]
              (list* tag attr kids))))))

(defn pp-forms
  [doctype forms]
  (str "<!DOCTYPE " doctype ">\n" (with-out-str (pprint forms))))
