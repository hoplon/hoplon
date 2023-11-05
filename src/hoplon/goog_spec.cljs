(ns hoplon.goog-spec
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

(defmethod hspec/do! :focus
  [_]
  (hspec/attr boolean?))

(defmethod hspec/do! :select
  [_]
  (hspec/attr boolean?))

(defmethod hspec/do! :focus-select
  [_]
  (hspec/attr boolean?))

(defmethod hspec/do! :text
  [_]
  (hspec/attr string?))

(defmethod hspec/do! :html
  [_]
  (hspec/attr string?))

(defmethod hspec/do! :scroll-to
  [_]
  (hspec/attr boolean?))

(defn spec! []
  (spect/instrument `h/-elem!)
  (spect/instrument `h/-do!)
  (spect/instrument `h/-on!))
