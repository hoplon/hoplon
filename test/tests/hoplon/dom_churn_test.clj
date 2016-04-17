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

(deftest add-remove-inputs-diff
  (add-remove-inputs! ["a" "b" "c"]))

(deftest add-remove-inputs-same
  (add-remove-inputs! (let [t "a"] [t t t])))

(deftest deterministic-loop-tpl
  (let [inputs                #(elements "#deterministic-loop-tpl input")
        nth-input             #(nth (inputs) %)
        nth-value             #(value (nth-input %))
        nth-value=            #(= %2 (nth-value %1))
        verify-state          #(do  (is (= (count %) (count (inputs))))
                                    ; The inputs need the same values as the current state vector.
                                    (dotimes [n (count %)]
                                      (is (nth-value= n (get % n)))))
        do-state!             #(do  (click %1)
                                    (verify-state %2))
        state-1!              #(do-state! "#deterministic-loop-tpl [data-state-1]" ["a" "a"])
        state-2!              #(do-state! "#deterministic-loop-tpl [data-state-2]" ["a"])
        ; We should be able to set the state to either state as many times as we
        ; want, in whatever order we want, and get the same result.
        toggle-states-a-bit!  #(let [states [state-1! state-2!]]
                                  (dotimes [_ (+ 5 (rand-int 10))]
                                    ((rand-nth states))))]
    (is (= 0 (count (inputs))))
    (toggle-states-a-bit!)
    (state-1!)
    (send-keys (nth-input 0) "\b")
    (toggle-states-a-bit!)
    (state-1!)
    (send-keys (nth-input 1) "\b")))
