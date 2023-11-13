(ns hoplon.class-test
 (:require
  [hoplon.core :as h]
  hoplon.goog-provider
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
  (is (.matches el ".foo"))

  (el :class "bar")
  (is (.matches el ".foo.bar"))

  (el :class/baz "baz")
  (is (.matches el ".foo.bar.baz"))

  (el :class {"foo" false
              "baz" false
              "bar" true})
  (is (.matches el ".bar"))
  (is (not (.matches el ".foo")))
  (is (not (.matches el ".baz"))))

 (is (.matches
      (h/div
       :class #{"foo" "bar"}
       :class/baz "baz"
       :class/bing #{"bing"})
      ".foo.bar.baz.bing")))

(deftest ??class--extend
 (is (.matches
      (extended-el :class "bing")
      "div.foo.bar.bing"))

 (is (.matches
      (extended-el :class/extend "baz")
      "div.foo.baz:not(.bar)")))
