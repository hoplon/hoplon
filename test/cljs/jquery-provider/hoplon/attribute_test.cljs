(ns hoplon.attribute-test
  (:require
    [hoplon.core :as h]
    hoplon.jquery
    [cljs.test :refer-macros [deftest is]]))

(deftest ??attribute
  (let [el (h/div :baz "foo")]
    (is (= (:baz el) "foo"))

    (is (= (:bar el "bar") "bar"))

    (is (= (get el :baz) "foo"))

    (is (= (get el :bar "bar") "bar"))))
