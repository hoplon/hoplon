(ns hoplon.jquery-test
 (:require
  [hoplon.core :as h]
  [javelin.core :as j]
  hoplon.dom
  hoplon.svg
  [cljs.test :refer-macros [deftest is]]))

(deftest ??svg-attributes
 ; jQuery does NOT support SVG attributes as it lowercases everything while SVG
 ; uses camelCase for quite a bit of the spec.
 ; The default behaviour for settings attributes works fine so all we have to do
 ; is let hoplon.core handle :svg/* instead of hoplon.jquery.
 ; https://github.com/hoplon/hoplon/issues/173
 (let [el (hoplon.svg/svg :svg/viewBox "0 0 10 10")]
  (is (= "0 0 10 10" (.getAttribute el "viewBox")))
  (is (nil? (.getAttribute el "viewbox")))))

(deftest ??check-val!
 (let [c (j/cell nil)
       checkbox (h/input :value c :type "checkbox")
       radio (h/input :value c :type "radio")]
  (doseq [el [checkbox radio]]
   ; try a false value
   (reset! c false)
   (is
    (not (.-checked el))
    "native is false when false")
   (is
    (not (:checked el))
    ":checked is false when false")
   (is
    (not (:value el))
    ":value is false when false")
   ; try a true value
   (reset! c true)
   (is
    (.-checked el)
    "native is true when true")
   ; @TODO implement :checked and :value lookups for jquery elems
   ; (is
   ;  (:checked el)
   ;  ":checked is true when true")
   ; (is
   ;  (:value el)
   ;  ":value is true when true")
   ; try reverting to nil
   (reset! c nil)
   (is
    (not (.-checked el))
    "native is false when nil")
   (is
    (not (:checked el))
    ":checked is false when nil")
   (is
    (not (:value el))
    ":value is false when nil"))))
