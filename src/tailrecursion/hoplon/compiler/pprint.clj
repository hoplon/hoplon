;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.compiler.pprint
  (:require
    [clojure.string :as string]
    [fipp.printer   :as printer :refer [defprinter]]))

(def void-elems
  #{'area     'base     'br       'col      'embed    'hr       'img    'input
    'keygen   'link     'menuitem 'meta     'param    'source   'track  'wbr})

(defprotocol IPretty
  (-pp [this]))

(defn lines
  [coll]
  (interpose :line (keep -pp coll)))

(extend-protocol IPretty
  nil
  (-pp [this] nil)
  
  clojure.lang.IPersistentVector
  (-pp [this]
    [:group "<clj_vector>" [:nest 2 (lines this)] "</clj_vector>"])
  
  clojure.lang.IPersistentMap
  (-pp [this]
    [:group "<clj_map>" [:nest 2 (lines (mapcat identity this))] "</clj_map>"])

  clojure.lang.IPersistentSet
  (-pp [this]
    [:group "<clj_set>" [:nest 2 (lines this)] "</clj_set>"])

  clojure.lang.Symbol
  (-pp [this]
    [:text (str "<" this "></" this ">")])

  clojure.lang.Keyword
  (-pp [this]
    [:group "<clj_keyword>" (name this) "</clj_keyword>"])

  java.lang.String
  (-pp [this]
    [:text (string/trim this)])

  java.lang.Number
  (-pp [this]
    [:group "<clj_number>" (str this) "</clj_number>"])

  clojure.lang.ISeq
  (-pp [this]
    (let [[tag & tail] this
          re-tag  #"^[a-zA-Z0-9-_.]+$"
          tag     (if (and (symbol? tag)
                           (or (re-find re-tag (str tag))
                               (contains? #{'$text '$comment} tag))) tag 'div)
          attr?   (and (map? (first tail)) (first tail)) 
          void?   (contains? void-elems tag)
          attr    (or attr? {})
          kids    (if attr? (rest tail) tail)
          to-str  #(if (string? %) % (pr-str %))
          pp-attr (fn [m]
                    (->> m
                      (map (fn [[k v]] (str (name k) "=" (pr-str (to-str v)))))
                      (interpose :line)))
          open    (if (seq attr)
                    [:group (str "<" tag) " " [:align (pp-attr attr)] ">"]
                    [:group (str "<" tag ">")])
          open    (if-not void? (conj open [:line ""]) open)]
      (case tag
        $text    (string/trim (apply str kids)) 
        $comment [:group "<!--" :line (lines kids) :line "-->"]
        (if-not void? 
          (reduce conj open [[:nest 2 (lines kids)] [:line ""] [:text "</" tag ">"]])
          open)))))

(defn pretty [x]
  (-pp x))

(defprinter pprint pretty {:width 110})
