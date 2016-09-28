(ns hoplon.prefix
  (:refer-clojure :exclude [symbol filter mask set])
  (:require
    [hoplon.core :refer [ensure-kids!]]))

;; Common Attribute Prefixes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod hoplon.core/do! :xlink/*
  [elem kw val]
  (let [xlink "http://www.w3.org/1999/xlink"]
    (.setAttributeNS elem xlink (name kw) val)))

(defmethod hoplon.core/do! :xsd/*
  [elem kw val]
  (let [xsd "http://www.w3.org/2001/XMLSchema"]
    (.setAttributeNS elem xsd (name kw) val)))

(defmethod hoplon.core/do! :xsi/*
  [elem kw val]
  (let [xsi "http://www.w3.org/2001/XMLSchema-instance"]
    (.setAttributeNS elem xsi (name kw) val)))

(defmethod hoplon.core/do! :xsl/*
  [elem kw val]
  (let [xsl "http://www.w3.org/1999/XSL/Transform"]
    (.setAttributeNS elem xsl (name kw) val)))

(defmethod hoplon.core/do! :math/*
  [elem kw val]
  (let [math "http://www.w3.org/1998/Math/MathML"]
    (.setAttributeNS elem math (name kw) val)))

(defmethod hoplon.core/do! :rdf/*
  [elem kw val]
  (let [rdf "https://www.w3.org/1999/02/22-rdf-syntax-ns"]
    (.setAttributeNS elem rdf (name kw) val)))

(defmethod hoplon.core/do! :rdfs/*
  [elem kw val]
  (let [rdfs "http://www.w3.org/2000/01/rdf-schema"]
    (.setAttributeNS elem rdfs (name kw) val)))
