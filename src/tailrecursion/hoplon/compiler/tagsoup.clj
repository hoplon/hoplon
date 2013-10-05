(ns tailrecursion.hoplon.compiler.tagsoup
  (:require
    [pl.danieljanus.tagsoup               :as ts]
    [tailrecursion.hoplon.compiler.pprint :as pp]))

(def parse ts/parse)
(def parse-string ts/parse-string)
(def children ts/children)

(defn script?
  [form]
  (and (vector? form) (= :script (first form))))

(defn hoplon-script?
  [form]
  (and (script? form) (= "text/hoplon" (:type (second form)))))

(def hoplon-script-include (comp :include second))

(defn tagsoup->hoplon-1
  "Given a tagsoup/hiccup data structure elem, returns the corresponding list
  of hoplon forms."
  [elem]
  (cond
    (string? elem)
    (list (list '$text elem)) 

    (hoplon-script? elem)
    (read-string (str "(" (nth elem 2) ")"))
    
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
        forms  (tagsoup->hoplon-1 elem)]
    (if (= 'html (first* (last* (first* forms))))
      (rest* (first forms))
      forms)))

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
