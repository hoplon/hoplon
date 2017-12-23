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
                          (.createElement js/document "div")]]]
  (.appendChild parent child)
  (is (= parent (.-parentNode child)))
  (doseq [n [child parent]]
   (when (instance? js/Element n)
    (h/native? n))
   (is (h/native-node? n))
   (is (not (h/managed? n))))))

 ; if any node is managed
