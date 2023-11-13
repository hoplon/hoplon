(ns hoplon.attribute-test
  (:require
    [hoplon.core :as h]
    hoplon.jquery
    [cljs.test :refer-macros [deftest is]]))

(deftest ??attribute
  (let [el (h/div :bazz "foo")]
    (is (= (:bazz el) "foo"))

    (is (= (:bar el "bar") "bar"))

    (is (= (get el :bazz) "foo"))

    (is (= (get el :bar "bar") "bar"))))
