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

  (el :baz/class "baz")
  (is (.webkitMatchesSelector el ".foo.bar.baz"))

  (is (.webkitMatchesSelector
       (h/div
        :class #{"foo" "bar"}
        :baz/class "baz")
       ".foo.bar.baz"))))
