(ns hoplon.prototype-test
 (:require
  [hoplon.core :as h]
  hoplon.jquery
  [javelin.core :as j]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??appendChild--internal-remove
 ; native behaviour when appending child to new parent
 (let [parent-1 (.createElement js/document "div")
       parent-2 (.createElement js/document "div")
       child (.createElement js/document "div")]
  (.appendChild parent-1 child)
  (is (= parent-1 (.-parentNode child)))

  (.appendChild parent-2 child)
  (is (= parent-2 (.-parentNode child)))
  (is (nil? (array-seq (.-childNodes parent-1))))
  (is (= [child] (array-seq (.-childNodes parent-2)))))

 ; managed append child internally removes children with existing parents
 (let [parent-1 (h/div)
       parent-2 (h/div)
       child (h/div)]
  (.appendChild parent-1 child)
  (is (= parent-1 (.-parentNode child)))

  (.appendChild parent-2 child)
  (is (= parent-2 (.-parentNode child)))
  (is (nil? (array-seq (.-childNodes parent-1))))
  (is (= [child] (array-seq (.-childNodes parent-2))))))

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
  (is (= [child] (array-seq (.-childNodes parent))))

  (doseq [n [child parent]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/element? n))))

  (is (= child (.removeChild parent child)))
  (is (nil? (.-parentNode child)))
  (is (nil? (array-seq (.-childNodes parent))))

  (doseq [n [child parent]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/element? n)))))

 ; if the parent is native, but the child is managed but not a cell then the
 ; parent continues to be native and child continues as managed
 (doseq [[parent child] [[(.createElement js/document "div")
                          (h/div)]]]
  (is (= child (.appendChild parent child)))
  (is (= parent (.-parentNode child)))

  (is (h/native? parent))
  (is (h/native-node? parent))
  (is (not (h/element? parent)))

  (is (not (h/native? child)))
  (is (not (h/native-node? child)))
  (is (h/element? child))

  (is (= child (.removeChild parent child)))
  (is (nil? (.-parentNode child)))
  (is (nil? (array-seq (.-childNodes parent))))

  (is (h/native? parent))
  (is (h/native-node? parent))
  (is (not (h/element? parent)))

  (is (not (h/native? child)))
  (is (not (h/native-node? child)))
  (is (h/element? child))

  ; if the parent is native but the child is a cell then the parent becomes
  ; managed and the child remains as a cell
  ; management requires upgrading the element
  (doseq [[parent child] [[(.createElement js/document "div")
                           (j/cell "foo")]
                          [(.createElement js/document "div")
                           (j/cell= "foo")]
                          [(.createElement js/document "div")
                           (j/cell= (h/div "foo"))]]]
   ; parent is initially native
   (is (h/native? parent))
   (is (h/native-node? parent))
   (is (not (h/element? parent)))

   ; child initially a cell
   (is (j/cell? child))

   (is (= child (h/append-child! parent child)))
   (is (= "foo" (.-textContent parent)))
   (if (string? @child)
    (is
     (= [@child]
      (map
       #(.-nodeValue %)
       (array-seq (.-childNodes parent)))))
    (is (= [@child] (array-seq (.-childNodes parent)))))

   ; parent becomes managed
   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   ; child is still cell
   (is (j/cell? child))

   (is (= child (h/remove-child! parent child)))
   (is (= "" (.-textContent parent)))
   (is (nil? (array-seq (.-childNodes parent))))

   ; parent is still managed
   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   ; child is still cell
   (is (j/cell? child)))

  ; if the parent is managed and the child is anything then both will continue
  ; as they were after appending.
  (let [parent (h/div)
        child (.createElement js/document "div")]
   (is (= child (h/append-child! parent child)))
   (is (= parent (.-parentNode child)))
   (is (= [child] (array-seq (.-childNodes parent))))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   (is (h/native? child))
   (is (h/native-node? child))
   (is (not (h/element? child)))

   (is (= child (h/remove-child! parent child)))
   (is (nil? (.-parentNode child)))
   (is (nil? (array-seq (.-childNodes parent))))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   (is (h/native? child))
   (is (h/native-node? child))
   (is (not (h/element? child))))

  (let [parent (h/div)
        child (h/div)]
   (is (= child (h/append-child! parent child)))
   (is (= parent (.-parentNode child)))
   (is (= [child] (array-seq (.-childNodes parent))))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (h/element? child))

   (is (= child (.removeChild parent child)))
   (is (nil? (.-parentNode child)))
   (is (nil? (array-seq (.-childNodes parent))))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (h/element? child)))

  (let [parent (h/div)
        child (j/cell (h/div))]
   (is (= child (h/append-child! parent child)))
   (is (= parent (.-parentNode @child)))
   (is (= [@child] (array-seq (.-childNodes parent))))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (not (h/element? child)))
   (is (j/cell? child))

   (is (= child (h/remove-child! parent child)))
   (is (nil? (.-parentNode parent)))
   (is (nil? (array-seq (.-childNodes parent))))

   (is (not (h/native? parent)))
   (is (not (h/native-node? parent)))
   (is (h/element? parent))

   (is (not (h/native? child)))
   (is (not (h/native-node? child)))
   (is (not (h/element? child)))
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

(deftest ??insertBefore
 ; if all involved nodes are native, there should be no hoplon management
 (doseq [[parent x y] [[(.createElement js/document "div")
                        (.createElement js/document "div")
                        (.createElement js/document "div")]
                       [(.createElement js/document "body")
                        (.createTextNode js/document "foo")
                        (.createTextNode js/document "bar")]
                       [(.createElement js/document "body")
                        (.createElement js/document "div")
                        (.createTextNode js/document "foo")]
                       [(.createElement js/document "head")
                        (.createComment js/document "foo")
                        (.createComment js/document "foo")]
                       [(.createElement js/document "head")
                        (.createElement js/document "div")
                        (.createElement js/document "div")]
                       [(.createElement js/document "body")
                        (h/$text "foo")
                        (h/$text "foo")]]]
  (is (= y (.appendChild parent y)))
  (is (= parent (.-parentNode y)))

  (is (= x (.insertBefore parent x y)))
  (is (= parent (.-parentNode x)))

  (is (= [x y] (array-seq (.-childNodes parent))))

  (doseq [n [parent x y]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/element? n)))))

 ; if the parent is managed then the insert will be managed by hoplon
 (doseq [[parent x y] [[(h/div)
                        (h/div)
                        (h/div)]
                       [(h/div)
                        (.createElement js/document "div")
                        (.createElement js/document "div")]
                       [(h/div)
                        (h/div)
                        (.createElement js/document "div")]]]
  (is (= y (.appendChild parent y)))
  (is (= parent (.-parentNode y)))

  (is (= x (.insertBefore parent x y)))
  (is (= parent (.-parentNode x)))

  (is (= [x y] (array-seq (.-childNodes parent))))

  (is (h/element? parent))
  (is (not (h/native? parent)))
  (is (not (h/native-node? parent)))))

 ; insertBefore is broken for cells
 ; https://github.com/hoplon/hoplon/issues/207
 ; (let [parent (h/div)
 ;       x (j/cell "foo")
 ;       y (j/cell "bar")]
 ;  (is (= y (.appendChild parent y)))
 ;  (is (= x (.insertBefore parent x y)))
 ;
 ;  (is (= "foobar" (.-textContent parent))))
 ;
 ; (let [parent (h/div)
 ;       x (j/cell "foo")
 ;       y (h/div "bar")]
 ;  (is (= y (.appendChild parent y)))
 ;  (is (= x (.insertBefore parent x y)))
 ;
 ;  (is (= "foobar" (.-textContent parent))))
 ;
 ; (let [parent (h/div)
 ;       x (h/div "foo")
 ;       y (j/cell "bar")]
 ;  (is (= y (.appendChild parent y)))
 ;  (is (= x (.insertBefore parent x y)))
 ;
 ;  (is (= "foobar" (.-textContent parent)))))

(deftest ??replaceChild
 ; if all involved nodes are native, there should be no hoplon management
 (doseq [[parent x y] [[(.createElement js/document "div")
                        (.createElement js/document "div")
                        (.createElement js/document "div")]
                       [(.createElement js/document "body")
                        (.createTextNode js/document "foo")
                        (.createTextNode js/document "bar")]
                       [(.createElement js/document "body")
                        (.createElement js/document "div")
                        (.createTextNode js/document "foo")]
                       [(.createElement js/document "head")
                        (.createComment js/document "foo")
                        (.createComment js/document "foo")]
                       [(.createElement js/document "head")
                        (.createElement js/document "div")
                        (.createElement js/document "div")]
                       [(.createElement js/document "body")
                        (h/$text "foo")
                        (h/$text "foo")]]]
  (is (= y (.appendChild parent y)))
  (is (= parent (.-parentNode y)))

  (is (= y (.replaceChild parent x y)))
  (is (= parent (.-parentNode x)))
  (is (nil? (.-parentNode y)))

  (is (= [x] (array-seq (.-childNodes parent))))

  (doseq [n [parent x y]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/element? n)))))

 ; if the parent is managed then the replacement will be managed by hoplon
 (doseq [[parent x y] [[(h/div)
                        (h/div)
                        (h/div)]
                       [(h/div)
                        (.createElement js/document "div")
                        (.createElement js/document "div")]
                       [(h/div)
                        (h/div)
                        (.createElement js/document "div")]]]
  (is (= y (.appendChild parent y)))
  (is (= parent (.-parentNode y)))

  (is (= y (.replaceChild parent x y)))
  (is (= parent (.-parentNode x)))

  (is (= [x] (array-seq (.-childNodes parent))))

  (is (h/element? parent))
  (is (not (h/native? parent)))
  (is (not (h/native-node? parent)))))

 ; replaceChild is broken for cells
 ; https://github.com/hoplon/hoplon/issues/207
 ; (let [parent (h/div)
 ;       x (j/cell "foo")
 ;       y (j/cell "bar")]
 ;  (is (= y (.appendChild parent y)))
 ;  (is (= y (.replaceChild parent x y)))
 ;
 ;  (is (= "foo" (.-textContent parent))))
 ;
 ; (let [parent (h/div)
 ;       x (j/cell "foo")
 ;       y (h/div "bar")]
 ;  (is (= y (.appendChild parent y)))
 ;  (is (= y (.replaceChild parent x y)))
 ;
 ;  (is (= "foo" (.-textContent parent))))
 ;
 ; (let [parent (h/div)
 ;       x (h/div "foo")
 ;       y (j/cell "bar")]
 ;  (is (= y (.appendChild parent y)))
 ;  (is (= y (.replaceChild parent x y)))
 ;
 ;  (is (= "foo" (.-textContent parent)))))
