(ns hoplon.app-test
  (:require
    [clj-webdriver.taxi :as taxi]
    [clojure.test         :refer [deftest is use-fixtures]]
    [hoplon.app-test.tags :refer [tags]]
    [hoplon.test-fixtures]))

(use-fixtures :each hoplon.test-fixtures/selenium-driver!)

(deftest all-elements
  "all html elements can be ouput by hoplon"
  (doseq [tag tags]
    (is (taxi/exists? tag))))
