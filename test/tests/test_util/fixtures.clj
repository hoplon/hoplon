(ns test-util.fixtures
  (:use
    [clj-webdriver.taxi]))

(defn selenium-driver!
  ([t] (selenium-driver! t "http://localhost:3000"))
  ([t to]
   (set-driver! {:browser :firefox} to)
   (t)
   (quit)))
