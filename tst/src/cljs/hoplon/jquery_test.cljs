(ns hoplon.jquery-test
 (:require
  hoplon.jquery
  hoplon.svg
  [cljs.test :refer-macros [deftest is]]))

(deftest ??svg-attributes
 ; jQuery does NOT support SVG attributes as it lowercases everything while SVG
 ; uses camelCase for quite a bit of the spec.
 ; The default behaviour for settings attributes works fine so all we have to do
 ; is nothing in this namespace and let hoplon.core handle :svg/*
 ; https://github.com/hoplon/hoplon/issues/173
 (let [el (hoplon.svg/svg :svg/viewBox "0 0 10 10")]
  (is (= "0 0 10 10" (.getAttribute el "viewBox")))
  (is (= nil (.getAttribute el "viewbox")))))
