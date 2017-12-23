(ns hoplon.prototype-test
 (:require
  [hoplon.core :as h]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??appendChild
 ; if all involved nodes are native, there should be no hoplon management
 (let [child (.createElement js/document "div")
       parent (.createElement js/document "div")]
  (.appendChild parent child)
  (is (= parent (.-parentNode child)))
  (doseq [n [child parent]]
   (is (h/native? n))
   (is (h/native-node? n))
   (is (not (h/managed? n))))))
