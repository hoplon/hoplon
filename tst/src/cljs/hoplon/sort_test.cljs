(ns hoplon.sort-test
 (:require
  hoplon.jquery
  [hoplon.core :as h]
  [javelin.core :as j]
  [cljs.test :refer-macros [deftest is]]))

(deftest ??sorting-elements
 (let [data (j/cell {:a 1 :b 1 :c 2})
       el (h/div
           (h/for-tpl [[k v] (j/cell= (sort-by
                                       (fn [[k v]] [v k])
                                       data))]

            (h/input
             :value (j/cell= (get data k))
             :input #(swap! data assoc @k @%))))
       read-vals (fn [el]
                  (map
                   #(-> % js/jQuery .val)
                   (-> el js/jQuery (.find "input") array-seq)))]
  (is (= ["1" "1" "2"] (read-vals el)))
  (-> el js/jQuery (.find "input") (.val 2) (.trigger "input"))
  (is (= ["1" "2" "2"] (read-vals el)))))
