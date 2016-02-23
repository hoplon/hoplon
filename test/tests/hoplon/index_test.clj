(ns hoplon.index-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]))

(set-driver! {:browser :firefox})

(def base-url "http://localhost:3000")

(deftest first-test
  (to base-url)
  (is (= "hello world" (text (element "h1"))))
  (quit)
)
