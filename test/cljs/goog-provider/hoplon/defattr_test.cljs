(ns hoplon.defattr-test
 (:require
  [hoplon.core :as h]
  hoplon.goog-provider
  [cljs.test :refer-macros [deftest is]]))

(h/defattr :bazz [elem key value]
  (elem :attr {:custombazz value}))

(deftest ??defattr
 (let [el (h/div :bazz true)]
  (is (.matches el "div[custombazz]"))))
