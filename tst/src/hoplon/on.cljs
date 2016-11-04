(ns+ hoplon.on
  (:page "on.html")
  (:require [javelin.core :refer [cell]]
            [hoplon.core :refer :all]))

(defn click-bind-test-div
  []
  (let [c (cell nil)
        f #(reset! c true)]
    (div :click f :data-c c "Click me!" :id "click-bind-test-div")))

(html
  (head)
  (body
    (click-bind-test-div)))
