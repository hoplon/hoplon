(ns hoplon.defelem-test
 (:require
  [hoplon.core :as h]
  hoplon.jquery
  [cljs.test :refer-macros [deftest is]]))

(h/defelem div--basic
 [_ _]
 (h/div))

(h/defelem div--attributes
 [attributes _]
 (h/div attributes))

(h/defelem div--destructured
 [{:keys [foo] :as attributes} _]
 (h/div
  :data-bar foo
  (dissoc attributes :foo)))

(h/defelem div--destructured-attributes-only
 [{:keys [foo] :as attributes}]
 (h/div
  :data-bar foo
  (dissoc attributes :foo)))

(h/defelem div--children
 [_ children]
 (h/div children))

(h/defelem div--attributes-children
 [attributes children]
 (h/div attributes children))

(deftest ??divs
 ; trivial case
 (is (.matches (div--basic) "div"))

 ; attribute arguments
 (is (.matches (div--attributes :data-foo true) "div[data-foo]"))

 ; destructuring attributes
 (is (.matches (div--destructured :foo "123" :data-baz "456") "div[data-bar=\"123\"][data-baz=\"456\"]"))
 (is (.matches (div--destructured-attributes-only :foo "123" :data-baz "456") "div[data-bar=\"123\"][data-baz=\"456\"]"))

 (doseq [el [; children arguments
             (div--children (h/span) (h/p))
             ; a vector of children
             (div--children [(h/span) (h/p)])]]
  (is (.querySelector el "span"))
  (is (.querySelector el "p")))

 (doseq [el [; positional arguments
             (div--attributes-children :data-foo true (h/span) (h/p))
             ; data arguments
             (div--attributes-children {:data-foo true} [(h/span) (h/p)])]]))
