(ns hoplon.on-test
  (:require
    [clj-webdriver.taxi :as taxi]
    [clojure.test :refer [deftest is use-fixtures]]
    [hoplon.test-fixtures]))

(use-fixtures :each #(hoplon.test-fixtures/selenium-driver! % "http://localhost:3020/on.html"))

(deftest click-binding
  (let [el (taxi/element "#click-bind-test-div")]
    (is (nil? (taxi/attribute el "data-c")))
    (taxi/click el)
    (is (= "data-c" (taxi/attribute el "data-c")))))
