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
    [pl.danieljanus.tagsoup :as ts]
    [clojure.pprint         :as pp :refer [pprint]]
    [clojure.string         :as cs :refer [blank? replace replace-first split join]]
    [clojure.walk           :as cw :refer [postwalk]]))

(def cljs-attr? #(and (string? %) (re-find #"^\s*\{\{.*\}\}\s*" %)))
(def walk-attr  #(if-not (cljs-attr? %) % (-> % (replace #"^\s*\{\{\s*" "")
                                            (replace #"\s*\}\}\s*$" "")
                                            read-string)))

(defn split-ns [x]
  (let [[_ ns? & _ :as p] (split (name x) #"\.")]
    [(when ns? (join "." (butlast p))) (last p)]))

(defn ns-keys [attr]
  (into {} (for [[k v] attr] [(apply keyword (split-ns k)) v])))

(defn parse-hiccup [x]
  (if (string? x)
    x
    (let [[tag attr & kids] x]
      (list*
        (apply symbol (split-ns tag))
        (concat
          (if (empty? attr) (list) (list (ns-keys attr))) 
          (map parse-hiccup kids))))))

(defn collapse [x]
  (if (string? x)
    x
    (let [[tag attr & kids] x
          ws? #(and (string? %) (blank? %))]
      (into [tag attr] (map collapse (if (= :pre tag) kids (remove ws? kids)))))))

(defn read-hiccup [s]
  (collapse (ts/parse-string s :strip-whitespace false)))

(defn parse-snip [s]
  (let [[html attr body] (read-hiccup s)]
    (parse-hiccup (nth body 2))))

(defn parse-page [s]
  (parse-hiccup (read-hiccup s)))

(defn parse-string [s]
  (let [hiccup  (read-hiccup s)
        prelude (read-string (str "(" (-> hiccup (nth 2) (nth 2)) ")"))]
    (concat prelude (list (parse-hiccup (postwalk walk-attr (nth hiccup 3)))))))

(defn parse [f]
  (parse-string (slurp f)))

(defn pedanticize [form]
  (cond (string? form) (list '$text form)
        (symbol? form) (list form {})
        (seq? form)
        (let [[tag & tail] form] 
          (if (or (= '$text tag) (= '$comment tag))
            form
            (let [attr (if (map? (first tail)) (first tail) {}) 
                  kids (map pedanticize (if (map? (first tail)) (rest tail) tail))]
              (list* tag attr kids))))))

(def void-elems
  #{'area     'base     'br       'col      'embed    'hr       'img    'input
    'keygen   'link     'menuitem 'meta     'param    'source   'track  'wbr})

(defn attr->string [attr]
  (->> (for [[k v] attr] 
         (str (name k) "=" (pr-str (if (string? v) v (str "{{ " v " }}"))))) 
       (cons "")
       (interpose " ")
       (apply str)))

(defn html-escape [s]
  (-> s (replace #"&" "&amp;") (replace #"<" "&lt;") (replace #">" "&gt;")))

(defn hoplon->string* [[tag attr & kids :as form]]
  (case tag
    $text     (html-escape attr)
    $comment  (str "<!-- " attr " -->")
    (let [attr (attr->string attr)
          kids (->> kids (map hoplon->string*) (apply str))]
      (if (void-elems tag)
        (str "<" tag attr ">")
        (str "<" tag attr ">" kids "</" tag ">")))))

(defn print-string [form]
  (hoplon->string* (pedanticize form)))

(defn print-page [doctype forms]
  (str "<!DOCTYPE " doctype ">\n" (print-string forms)))
