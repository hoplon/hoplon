(ns hoplon.index-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]
    [clojure.java.shell :only [sh]]))

(set-driver! {:browser :firefox})

(def base-url "http://localhost:3000")

(deftest foo
  (to base-url)
  (is (= "hello world" (text (element "h1"))))
  (quit)
)
