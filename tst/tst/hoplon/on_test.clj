(ns hoplon.on-test
  (:require
    [clj-webdriver.taxi :as taxi]
    [clojure.test :refer [deftest is use-fixtures]]
    [hoplon.test-fixtures]))

(use-fixtures :each #(hoplon.test-fixtures/selenium-driver! % "http://localhost:3020/on.html"))

(deftest click-binding
  (let [id "#click-bind-test-div"
        a "[data-c]"
        clicked? #(taxi/exists? (str id a))]
    (is (not (clicked?)))
    (taxi/click id)
    (is (clicked?))))
