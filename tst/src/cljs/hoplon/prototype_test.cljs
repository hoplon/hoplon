(ns hoplon.prototype-test
 (:require
  [hoplon.core :as h]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??appendChild
 ; if all involved nodes are native, there should be no hoplon management
 (doseq [[parent child] [[(.createElement js/document "div")
                          (.createElement js/document "div")]
                         [(.createElement js/document "body")
                          (.createTextNode js/document "foo")]
                         [(.createElement js/document "body")
                          (.createElement js/document "div")]
                         [(.createElement js/document "head")
                          (.createComment js/document "foo")]
                         [(.createElement js/document "head")
                          (.createElement js/document "div")]
                         [(.createElement js/document "body")
                          (h/$text "foo")]]]
  (.appendChild parent child)
  (is (= parent (.-parentNode child)))
  (doseq [n [child parent]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/managed? n)))))

 ; if the parent is native, but the child is managed but not a cell then the
 ; parent continues to be native and child continues as managed.
 (doseq [[parent child] [[(.createElement js/document "div")
                          (h/div)]]]
  (.appendChild parent child)
  (is (= parent (.-parentNode child)))
  (is (h/native? parent))
  (is (h/native-node? parent))
  (is (not (h/managed? parent)))

  (is (not (h/native? child)))
  (is (not (h/native-node? child)))
  (is (h/managed? child))))
