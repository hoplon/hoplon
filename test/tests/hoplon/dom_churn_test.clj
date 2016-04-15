(ns hoplon.dom-churn-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]
    [test-util.fixtures :as fixtures]))

(def test-url "http://localhost:3000/dom-churn.html")

(use-fixtures :each #(fixtures/selenium-driver! % test-url))

(defn- add-remove-inputs!
  [t]
  (let [inputs #(elements "#input-list input")
        nth-input #(nth (inputs) %)
        [one two three] t]
    (send-keys (nth-input 0) one)
    (send-keys (nth-input 1) two)
    (send-keys (nth-input 2) three)
    (is (= 4 (count (inputs))))
    (send-keys (nth-input 0) "\b")
    (is (= 3 (count (inputs))))
    (is (= two (value (nth-input 0))))
    (is (= three (value (nth-input 1))))
    (is (= "" (value (nth-input 2))))))

; We should get the same result adding and removing inputs regardless of values.

(deftest ^:wip add-remove-inputs-diff
  (add-remove-inputs! ["a" "b" "c"]))

(deftest ^:wip add-remove-inputs-same
  (add-remove-inputs! (let [t "a"] [t t t])))
