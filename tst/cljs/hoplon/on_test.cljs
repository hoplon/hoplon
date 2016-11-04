(ns hoplon.on-test
  (:require [cljs.test :refer-macros [deftest is]]
            [hoplon.core :as h]
            [javelin.core :as j]
            [cljsjs.jquery]))

(deftest bind-events
  (let [c (j/cell nil)
        f #(reset! c true)
        i (h/div :click f)
        $i (js/jQuery i)]

    ; Native event.
    (is (nil? @c))
    (.dispatchEvent i (js/Event. "click"))
    (is (= true @c))

    ; jQuery event.
    (reset! c nil)
    (is (nil? @c))
    (.trigger $i "click")
    (is (= true @c))))
