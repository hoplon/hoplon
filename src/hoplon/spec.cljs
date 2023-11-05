(ns hoplon.spec
  (:require [cljs.spec.alpha :as spec]
            [hoplon.core :as h]))

;; Attribute Specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(spec/def ::class
  (spec/or
    :map map?
    :string string?
    :collection (spec/coll-of (spec/or :keyword keyword? :string string?))))

(spec/def ::value
  (spec/or
    :string string?
    :boolean boolean?))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Attribute Provider Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn attr [vspec]
  (spec/cat :element any? :attribute keyword? :value vspec))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Attribute Provider Spec Multimethods ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dispatcher [[_elem key _val]]
  (if-let [n (namespace key)] (keyword n "*") key))

(defmulti elem! dispatcher :default ::default)

(defmethod elem! ::default [_] any?)

(defmulti do! dispatcher :default ::default)

(defmethod do! ::default [_] any?)

(defmulti on! dispatcher :default ::default)

(defmethod on! ::default [_] (attr fn?))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Attribute Provider Specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(spec/def ::elem! (spec/multi-spec elem! ::elem!))

(spec/def ::do! (spec/multi-spec do! ::do!))

(spec/def ::on! (spec/multi-spec on! ::on!))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(spec/fdef h/-do! :args :hoplon.spec/do! :ret any?)
(spec/fdef h/-on! :args :hoplon.spec/on! :ret any?)
(spec/fdef h/-elem! :args :hoplon.spec/elem! :ret any?)
