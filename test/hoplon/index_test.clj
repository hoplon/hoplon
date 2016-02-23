(ns hoplon.index-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]
    [clojure.java.shell :only [sh]]))

(set-driver! {:browser :firefox})
(def current-dir (-> (java.io.File. "") .getAbsolutePath))
(def base-url (str "file://" current-dir "/target/index.html"))

(deftest foo
  (prn base-url)
  (println (sh "ls" "-la" current-dir))
  (to base-url)
  (is (= "hello world" (text (element "h1"))))
  (quit)
)
