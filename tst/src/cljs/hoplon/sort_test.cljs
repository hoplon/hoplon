(ns hoplon.sort-test
 (:require
  hoplon.jquery
  [hoplon.core :as h]
  [javelin.core :as j]
  [cljs.test :refer-macros [deftest is async]]))

(deftest ??sorting-elements
 (async done
  (let [data (j/cell {:a 1 :b 1 :c 2})
        sorted-data (j/cell= (sort-by (fn [[k v]] [v k]) data))
        el (h/div
            (h/for-tpl [[k v] sorted-data]
             (h/input
              :data-k k
              :value (j/cell= (get data k))
              :input #(swap! data assoc @k (int @%)))))
        read-vals (fn [el]
                   (map
                    #(-> % js/jQuery .val)
                    (-> el js/jQuery (.find "input") array-seq)))
        read-ks (fn [el]
                 (map
                  #(-> % js/jQuery (.attr "data-k"))
                  (-> el js/jQuery (.find "input") array-seq)))]
   (-> js/document .-body (.appendChild el))

   (h/with-dom el
    (is (= ["1" "1" "2"] (read-vals el)))
    (is (= [":a" ":b" ":c"] (read-ks el)))

    (-> el js/jQuery (.find "input") .first (.val 3) (.trigger "input"))
    (is (= {:a 3 :b 1 :c 2} @data)) ; passes
    (is (= [":b" ":c" ":a"] (read-ks el))) ; passes
    (is (= ["1" "2" "3"] (read-vals el))) ; fails with ["3" "2" "3"]!
    (done)))))
