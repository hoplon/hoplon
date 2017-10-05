(ns hoplon.defattr-test
 (:require
  [hoplon.core :as h]
  hoplon.jquery
  [cljs.test :refer-macros [deftest is]]))

(h/defattr :baz [elem key value]
  (elem :attr {key value}))

(deftest ??defattr
 (let [el (h/div :baz true)]
  (is (.webkitMatchesSelector el "div[baz]"))

  ))
