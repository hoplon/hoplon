(ns hoplon.index-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]
    [test-util.fixtures :as fixtures]))

(use-fixtures :each fixtures/selenium-driver!)

(deftest first-test
  (is (= "hello world" (text (element "h1")))))
