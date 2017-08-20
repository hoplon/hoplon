(ns hoplon.tpl-test
 (:require
  [cljs.test :refer-macros [deftest is]]
  [hoplon.core :as h]
  [javelin.core :as j]))

(deftest ??if-tpl
 (let [c (j/cell true)
       el (h/div
           (h/if-tpl c
            (h/p :class "foo")
            (h/span :class "bar")))]
  (is (.querySelector el "p.foo"))
  (is (not (.querySelector el "span.bar")))

  (reset! c false)
  (is (not (.querySelector el "p.foo")))
  (is (.querySelector el "span.bar"))))
