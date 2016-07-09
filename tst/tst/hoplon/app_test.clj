(ns hoplon.app-test
  (:require
    [clj-webdriver.taxi :as taxi]
    [clojure.test         :refer [deftest is use-fixtures]]
    [hoplon.app-test.tags :refer [tags]]))

(defn selenium-driver!
  ([t]
   (selenium-driver! t "http://localhost:3020"))
  ([t to]
   (taxi/set-driver! {:browser :firefox} to)
   (t)
   (taxi/quit)))

(use-fixtures :each selenium-driver!)

(deftest all-elements
  "all html elements can be ouput by hoplon"
  (doseq [tag tags]
    (is (taxi/exists? tag))))
