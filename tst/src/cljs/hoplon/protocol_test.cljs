(ns hoplon.protocol-test
 (:require
  [hoplon.core :as h]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??element-IFn
 (let [el (h/div :class "foo" (h/p))]
  (is (.matches el "div.foo"))
  (is (.querySelector el "p"))

  ; the el itself should act as a fn that accepts attributes and children
  (el (h/span) :class "bar")
  (is (.matches el "div.foo.bar"))
  (is (.querySelector el "p"))
  (is (.querySelector el "span"))))

(deftest ??element-ILookup
 (let [c1 (h/p)
       c2 (h/span)
       el (h/div c1 c2 :data-foo "foo")]
  (is (= "foo" (:data-foo el)))
  (is (= "foo" (get el :data-foo)))
  (is (= c1 (get el 0)))
  (is (= c2 (get el 1)))))
