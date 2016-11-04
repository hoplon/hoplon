(ns hoplon.test-fixtures
  (:require
    [clj-webdriver.taxi :as taxi]))

(defn selenium-driver!
  ([t]
   (selenium-driver! t "http://localhost:3020"))
  ([t to]
   (taxi/set-driver! {:browser :firefox} to)
   (t)
   (taxi/quit)))
