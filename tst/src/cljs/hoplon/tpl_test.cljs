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

(deftest ??for-tpl
 (let [find-text (fn [el]
                   (map
                    #(.-textContent %)
                    (array-seq
                     (.querySelectorAll el "div"))))]
  ; the most common use-case is a sequence in a cell
  (let [c (j/cell [1 2 3])
        el (h/div
            (h/for-tpl [t c]
             (h/div t)))]
   (is (= ["1" "2" "3"]
        (find-text el)))
   (reset! c ["a" "b" "c"])
   (is (= ["a" "b" "c"]
        (find-text el))))

  ; we want to be able to handle regular (non-cell) sequences
  (let [ts ["x" "y" "z"]]
   (is (= ts
        (find-text
         (h/div
          (h/for-tpl [t ts]
           (h/div t)))))))

  ; we want to be able to handle empty sequences and nil
  (let [c (j/cell [])
        el (h/div
            (h/for-tpl [v (j/cell= (seq c))]
             (h/div v)))]
   (is (= []
        (find-text el))))))
