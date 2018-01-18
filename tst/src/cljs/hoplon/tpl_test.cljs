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
   (j/cell= (when item (name item))))))

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
  (is (= first-child (first (hoplon.test-util/find el "div"))))

  (hoplon.test-util/trigger! first-child "click")
  (is (not (hoplon.test-util/matches first-child "[data-expanded]")))

  ; removing :a should just change first-child to :d instead
  (hoplon.test-util/trigger! first-child "click")
  (reset! items [:d :b :c]) ; <-- currently errors as :a goes to nil inside first-child
  (is (= ["d" "b" "c"] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))
  (is (= "d" (hoplon.test-util/text first-child)))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))
  (is (hoplon.test-util/contains el first-child))))

(deftest ??keyed-for-tpl--sortable
 (let [items (j/cell [:a :b :c])
       el (h/div (h/keyed-for-tpl nil nil [i items] (expandable i)))
       first-child (first (hoplon.test-util/find el "div"))
       last-child (last (hoplon.test-util/find el "div"))]
  (is (not (.querySelector el "[data-expanded]")))

  (hoplon.test-util/trigger! first-child "click")
  (is (= "a" (hoplon.test-util/text first-child)) "First child is not a in starting position")
  (is (hoplon.test-util/matches first-child "[data-expanded]"))

  ; a should be expanded
  ; first-child should be a reference to the last child now (because it moved)
  ; the items should be in reverse order
  ; event handlers should not break
  (swap! items reverse)
  (is (= ["c" "b" "a"] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))

  (is (= "a" (hoplon.test-util/text first-child)) "First child lost content a")
  (is (= last-child (first (hoplon.test-util/find el "div"))))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))

  (hoplon.test-util/trigger! first-child "click")
  (is (not (hoplon.test-util/matches first-child "[data-expanded]")))

  ; removing :a should pop the first-child element out of el
  ; :a goes to nil internally
  (hoplon.test-util/trigger! first-child "click")
  (reset! items [:d :b :c])
  (is (= ["d" "b" "c"] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))
  (is (= "" (hoplon.test-util/text first-child)))
  (is (hoplon.test-util/matches first-child "[data-expanded]"))
  (is (not (hoplon.test-util/contains el first-child)))))

(deftest ??keyed-for-tpl--key-changes
 ; nothing should happen unless a key changes
 (let [items (j/cell [{:id 1 :x "foo"} {:id 2}])
       tpl (h/keyed-for-tpl nil :id [i items]
            (h/div (j/cell= (:x i))))
       before @tpl]
  (reset! items [{:id 1} {:id 2}])
  (is (= before @tpl))

  ; keys changing should be a new fragment in the tpl
  (reset! items [{:id 2} {:id 1}])
  (is (not (= before @tpl)))))

(deftest ??keyed-for-tpl--upstream-updates
 (let [items (j/cell [{:id 1 :x "foo"} {:id 2}])
       tpl (h/keyed-for-tpl nil :id [i (j/cell= (sort-by :id items))]
            (h/div (j/cell= (:x i))))
       el (h/div tpl)]
  (is (= ["foo" ""] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))

  (swap! items assoc 0 {:id 1 :x "bar"})
  (is (= ["bar" ""] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))

  (swap! items assoc 2 {:id 3 :x "baz"})
  (is (= ["bar" "" "baz"] (map hoplon.test-util/text (hoplon.test-util/find el "div"))))))
