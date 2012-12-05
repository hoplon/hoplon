(ns hlisp.tagsoup

  (:import (org.w3c.tidy Tidy) 
           (java.io StringReader StringWriter))
  
  (:use
    [clojure.walk             :only [postwalk]]
    [hiccup.element           :only [javascript-tag]]
    )

  (:require
    [hiccup.core              :as hu]
    [pl.danieljanus.tagsoup   :as ts]
    [clojure.string           :as string]))

(def parse ts/parse)
(def parse-string ts/parse-string)
(def children ts/children)

(defn html [elem]
  (hu/html elem) )

(defn script?
  [form]
  (and (vector? form) (= :script (first form))))

(defn hlisp-script?
  [form]
  (and (script? form) (= "text/hlisp" (:type (second form)))))

(defn other-script?
  [form]
  (and (script? form) ((complement hlisp-script?) form)))

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

(defn hlisp->tagsoup
  "Given a hlisp form, returns the corresponding tagsoup/hiccup data structure."
  [form]
  (cond
    (symbol? form)
    [(keyword form) {}]

    (seq? form)
    (let [[tag & tail]    form
          [attrs & kids]  (if (map? (first tail)) tail (cons {} tail))]
      (if (or (= (symbol "$text") tag) (= (symbol "$comment") tag))
        (apply str kids)
        (into [(keyword tag) attrs] (mapv hlisp->tagsoup kids))))

    :else
    form))

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

(defn pp-html
  [doctype html-str]
  (let [printer (doto (new Tidy)
                  (.setTidyMark     false)
                  (.setDocType      "omit")
                  (.setSmartIndent  true)
                  (.setShowWarnings false)
                  (.setQuiet        true))
        writer  (new StringWriter)
        reader  (new StringReader html-str)]
    (.parse printer reader writer)
    (str "<!DOCTYPE " doctype ">\n" writer)))

(comment
  (ts/parse-string "<html><head><title>foo</title></head><body></body></html>")
  (type (first (read-string "(<html><head><title>foo</title></head><body></body></html>)"))) 
  (ts/parse-string "(ns foo) ((body))")

  )
