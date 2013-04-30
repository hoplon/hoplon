(ns hlisp.tagsoup
  (:require
    [clojure.walk :as walk :refer [postwalk]]
    [hlisp.pprint :as pp]
    [pl.danieljanus.tagsoup :as ts]
    [clojure.string :as string]))

(def parse ts/parse)
(def parse-string ts/parse-string)
(def children ts/children)

(defn script?
  [form]
  (and (vector? form) (= :script (first form))))

(defn hlisp-script?
  [form]
  (and (script? form) (= "text/hlisp" (:type (second form)))))

(def hlisp-script-include (comp :include second))

(defn tagsoup->hlisp
  "Given a tagsoup/hiccup data structure elem, returns the corresponding list
  of hlisp forms."
  [elem]
  (cond
    (string? elem)
    (list (list '$text elem)) 

    (hlisp-script? elem)
    (read-string (str "(" (nth elem 2) ")"))
    
    :else
    (let [[t attrs & kids] elem
          tag   (symbol (name t)) 
          kids  (apply concat (map tagsoup->hlisp kids)) 
          expr  (concat (list tag) (when (seq attrs) (list attrs)) kids)]
      (list (if (< 1 (count expr)) expr (first expr))))))

(defn pedanticize
  [form]
  (cond
    (string? form)
    (list '$text form)

    (symbol? form)
    (list form {})

    (seq? form)
    (let [[tag & tail] form] 
      (if (or (= '$text tag) (= '$comment tag))
        form
        (let [attr (if (map? (first tail)) (first tail) {}) 
              kids (map pedanticize (if (map? (first tail)) (rest tail) tail))]
          (list* tag attr kids))))))

(defn pp-forms
  [doctype forms]
  (str "<!DOCTYPE " doctype ">\n" (with-out-str (pp/pprint forms))))
