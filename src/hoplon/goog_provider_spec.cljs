(ns hoplon.goog-provider-spec
  (:require [cljs.spec.test.alpha :as spect]
            [hoplon.core :as h]
            [hoplon.spec :as hspec]))

(defmethod hspec/do! :value
  [_]
  (hspec/attr any?))

(defmethod hspec/do! :class
  [_]
  (hspec/attr :hoplon.spec/class))

(defmethod hspec/do! :toggle
  [_]
  (hspec/attr boolean?))

(defmethod hspec/do! :fade-toggle
  [_]
  (hspec/attr boolean?))

(defmethod hspec/do! :text
  [_]
  (hspec/attr string?))

(defn spec! []
  (spect/instrument `h/-elem!)
  (spect/instrument `h/-do!)
  (spect/instrument `h/-on!))
