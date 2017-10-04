(ns hoplon.class-test
 (:require
  [hoplon.core :as h]
  hoplon.jquery
  [cljs.test :refer-macros [deftest is]]))

(deftest ??class
 (let [el (h/div :class "foo")]
  (is (.webkitMatchesSelector el ".foo"))

  (el :class "bar")
  (is (.webkitMatchesSelector el ".foo.bar"))

  (el :class/baz "baz")
  (is (.webkitMatchesSelector el ".foo.bar.baz"))

  (el :class {"foo" false
              "baz" false
              "bar" true})
  (is (.webkitMatchesSelector el ".bar"))
  (is (not (.webkitMatchesSelector el ".foo")))
  (is (not (.webkitMatchesSelector el ".baz"))))

 (is (.webkitMatchesSelector
      (h/div
       :class #{"foo" "bar"}
       :class/baz "baz"
       :class/bing #{"bing"})
      ".foo.bar.baz.bing")))
