(ns hoplon.on-test
  (:require [cljs.test :refer-macros [deftest is]]
            [hoplon.core :as h]
            [javelin.core :as j]))

(deftest bind-events
  (let [c (j/cell nil)
        f #(reset! c true)
        i (h/div :click f)
        e (js/Event. "click")]
    (is (nil? @c))
    (.dispatchEvent i e)
    (is (= true @c))))
