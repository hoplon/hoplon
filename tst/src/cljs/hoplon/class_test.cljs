(ns hoplon.class-test
 (:require
  [hoplon.core :as h]
  hoplon.jquery
  [cljs.test :refer-macros [deftest is]]))

(h/defelem base-el
 [attributes children]
 (h/div
  :class/base "foo"
  attributes
  children))

(h/defelem extended-el
 [attributes children]
 (base-el
  :class/extend "bar"
  attributes
  children))

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

(deftest ??class--extend
 (is (.webkitMatchesSelector
      (extended-el :class "bing")
      "div.foo.bar.bing"))

 (is (.webkitMatchesSelector
      (extended-el :class/extend "baz")
      "div.foo.baz:not(.bar)")))
