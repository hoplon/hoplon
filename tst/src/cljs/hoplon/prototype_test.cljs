(ns hoplon.prototype-test
 (:require
  [hoplon.core :as h]
  [javelin.core :as j]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??append-remove
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
  (is (= child (.appendChild parent child)))
  (is (= parent (.-parentNode child)))
  (doseq [n [child parent]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/managed? n))))

  (is (= child (.removeChild parent child)))
  (is (nil? (.-parentNode child)))
  (doseq [n [child parent]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/managed? n)))))

 ; if the parent is native, but the child is managed but not a cell then the
 ; parent continues to be native and child continues as managed
 (doseq [[parent child] [[(.createElement js/document "div")
                          (h/div)]]]
  (is (= child (.appendChild parent child)))
  (is (= parent (.-parentNode child)))

  (is (h/native? parent))
  (is (h/native-node? parent))
  (is (not (h/managed? parent)))

  (is (not (h/native? child)))
  (is (not (h/native-node? child)))
  (is (h/managed? child))

  (is (= child (.removeChild parent child)))
  (is (nil? (.-parentNode child)))

  (is (h/native? parent))
  (is (h/native-node? parent))
  (is (not (h/managed? parent)))

  (is (not (h/native? child)))
  (is (not (h/native-node? child)))
  (is (h/managed? child))

  ; if the parent is native but the child is a cell then the parent becomes
  ; managed and the child remains as a cell
  (doseq [[parent child] [[(.createElement js/document "div")
                           (j/cell "foo")]
                          [(.createElement js/document "div")
                           (j/cell= "foo")]
                          [(.createElement js/document "div")
                           (j/cell= (h/div "foo"))]]]
   ; parent is initially native
   (is (h/native? parent))
   (is (h/native-node? parent))
   (is (not (h/managed? parent)))

   ; child initially a cell
   (is (j/cell? child))

   (is (= child (.appendChild parent child)))
   (is (= "foo" (.-textContent parent)))

   ; parent becomes managed
   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   ; child is still cell
   (is (j/cell? child))

   ; removing cells is broken, must deref
   ; https://github.com/hoplon/hoplon/issues/206
   (is (= @child (.removeChild parent @child)))
   (is (= "" (.-textContent parent)))

   ; parent is still managed
   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   ; child is still cell
   (is (j/cell? child)))

  ; if the parent is managed and the child is anything then both will continue
  ; as they were after appending.
  (let [parent (h/div)
        child (.createElement js/document "div")]
   (is (= child (.appendChild parent child)))
   (is (= parent (.-parentNode child)))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   (is (h/native? child))
   (is (h/native-node? child))
   (is (not (h/managed? child)))

   (is (= child (.removeChild parent child)))
   (is (nil? (.-parentNode child)))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   (is (h/native? child))
   (is (h/native-node? child))
   (is (not (h/managed? child))))

  (let [parent (h/div)
        child (h/div)]
   (is (= child (.appendChild parent child)))
   (is (= parent (.-parentNode child)))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (h/managed? child))

   (is (= child (.removeChild parent child)))
   (is (nil? (.-parentNode child)))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (h/managed? child)))

  (let [parent (h/div)
        child (j/cell (h/div))]
   (is (= child (.appendChild parent child)))
   (is (= parent (.-parentNode @child)))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (not (h/managed? child)))
   (is (j/cell? child))

   ; removing cells is broken, must deref
   ; https://github.com/hoplon/hoplon/issues/206
   (is (= @child (.removeChild parent @child)))
   (is (nil? (.-parentNode parent)))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/managed? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (not (h/managed? child)))
   (is (j/cell? child)))))
   
(deftest ??removeChild--non-child-error
 ; removing a non-child is an error
 (doseq [[a b] [[(.createElement js/document "div")
                 (.createElement js/document "div")]
                [(h/div)
                 (h/div)]
                [(h/div)
                 (j/cell (h/div))]]]
  (is (thrown? js/Error (.removeChild a b)))))
