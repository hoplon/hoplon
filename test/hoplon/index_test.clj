(ns hoplon.index-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]))

(set-driver! {:browser :firefox})
(def base-url (str "file://" (-> (java.io.File. "") .getAbsolutePath) "/target/index.html"))

(deftest foo
  (prn base-url)
  (to base-url)
  (is (= "hello world" (text (element "h1"))))
  (quit)
)
