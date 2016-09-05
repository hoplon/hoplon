(ns hoplon.binding
  (:refer-clojure :exclude [binding])
  (:require-macros [hoplon.binding :as b]))

(def thread-bindings "Stack of binding maps." (atom []))

(defn push-thread-bindings
  "Given a map with munged js variable names (as strings) for keys and the
  binding values as values, sets the variables to their new values and adds
  the binding map to the thread-bindings stack. If there are aren't yet any
  bindings for a variable its current value is stored in the global-bindings
  map so it can be restored later."
  [binding-map]
  (swap! thread-bindings conj binding-map)
  (doseq [{:keys [push!]} (vals binding-map)] (push!)))

(defn pop-thread-bindings
  "Pops the topmost binding map from thread-bindings stack and restores the
  variables to their previous saved states."
  []
  (let [popped (peek @thread-bindings)]
    (swap! thread-bindings pop)
    (doseq [{:keys [pop!]} (vals popped)] (pop!))))

(defn bound-fn*
  "Given a function f, returns a new function capturing the current bindings
  in its closure. When the returned function is invoked the saved bindings
  are pushed and set, f is applied to the arguments, and bindings are restored
  to their previous values."
  [f]
  (let [binding-map (apply merge @thread-bindings)]
    (fn [& args]
      (push-thread-bindings binding-map)
      (try (apply f args) (finally (pop-thread-bindings))))))
