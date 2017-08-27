(ns hoplon.protocol-test
 (:require
  [hoplon.core :as h]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??element-IFn
 (let [el (h/div :class "foo" (h/p))]
  (is (.webkitMatchesSelector el "div.foo"))
  (is (.querySelector el "p"))

  (el (h/span) :class "bar")
  (is (.webkitMatchesSelector el "div.foo.bar"))
  (is (.querySelector el "p"))
  (is (.querySelector el "span"))))
