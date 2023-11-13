(ns hoplon.defattr-test
 (:require
  [hoplon.core :as h]
  hoplon.dom
  [cljs.test :refer-macros [deftest is]]))

(h/defattr :baz [elem key value]
  (elem :attr {:custombaz value}))

(deftest ??defattr
 (let [el (h/div :baz true)]
  (is (.matches el "div[custombaz]"))))
