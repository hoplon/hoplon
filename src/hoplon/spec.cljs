(ns hoplon.spec
  (:require [cljs.spec.alpha :as spec]))

;; Type Specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(spec/def ::boolean boolean?)

(spec/def ::string string?)

(spec/def ::map map?)

;; Attribute Specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(spec/def ::elem any?)

(spec/def ::attr keyword?)

(spec/def ::class
  (spec/or
    :map map?
    :string string?
    :collection (spec/coll-of (spec/or :keyword keyword? :string string?))))

(spec/def ::value
  (spec/or
    :string ::string
    :boolean ::boolean))

;; Attribute Provider Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn attr [vspec]
  (spec/cat :element ::elem :attribute ::attr :value vspec))

;; Attribute Provider Spec Multimethods ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti do!
  (fn [[elem key val]]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(defmethod do! ::default
  [_]
  any?)

(defmulti on!
  (fn [[elem key val]]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(defmethod on! ::default
  [_]
  (attr fn?))

;; Attribute Provider Specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(spec/def ::do! (spec/multi-spec do! ::do!))

(spec/def ::on! (spec/multi-spec on! ::on!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
