(ns hoplon.tpl-test
 (:require
  hoplon.test-util
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
  (let [c (j/cell [])]
   (is (= []
        (find-text
         (h/div
          (h/for-tpl [v c]
           (h/div v))))))
   (is (= []
        (find-text
         (h/div
          (h/for-tpl [v (j/cell= (seq c))]
           (h/div v)))))))

  ; we need to handle dynamic length cells
  (let [c (j/cell ["1" "2" "3"])
        el (h/div
            (h/for-tpl [n c]
             (h/div n)))]
   (is (= ["1" "2" "3"]
        (find-text el)))
   (reset! c ["1" "2"])
   (is (= ["1" "2"]
        (find-text el)))
   (reset! c ["1" "2" "4" "5"])
   (is (= ["1" "2" "4" "5"]
        (find-text el))))))

; sorting

(defn expandable
 [item]
 (let [expand? (j/cell false)]
  (h/div
   :data-expanded expand?
   :click #(swap! expand? not)
   (j/cell= (name item)))))

(deftest ??for-tpl--not-sortable
 (let [items (j/cell [:a :b :c])
       el (h/div (h/for-tpl [i items] (expandable i)))
       first-child (first (hoplon.test-util/find el "div"))]
  (is (not (.querySelector el "[data-expanded]")))

  (hoplon.test-util/trigger! first-child "click")
  (is (= "a" (hoplon.test-util/text first-child)))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))

  ; c should be expanded (it is positional in for-tpl)
  ; first-child should still reference the first child (nothing moves)
  ; the item text should be in reverse order (it is re-derived in expandable)
  ; event handlers should not break
  (swap! items reverse)
  (is (= "c" (hoplon.test-util/text first-child)))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))

  (hoplon.test-util/trigger! first-child "click")
  (is (not (hoplon.test-util/matches first-child "[data-expanded]")))))

(deftest ??keyed-for-tpl--sortable
 (let [items (j/cell [:a :b :c])
       el (h/div (h/keyed-for-tpl identity [i items] (expandable i)))
       first-child (first (hoplon.test-util/find el "div"))]
  (is (not (.querySelector el "[data-expanded]")))

  (hoplon.test-util/trigger! first-child "click")
  (is (= "a" (hoplon.test-util/text first-child)))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))

  ; a should be expanded
  ; first-child should be a reference to the last child now (because it moved)
  ; the items should be in reverse order
  ; event handlers should not break
  (swap! items reverse)
  (is (= ["c" "b" "a"] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))
  (is (= "a" (hoplon.test-util/text first-child)))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))

  (hoplon.test-util/trigger! first-child "click")
  (is (not (hoplon.test-util/matches first-child "[data-expanded]")))))
