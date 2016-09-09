(ns hoplon.binding
  (:refer-clojure :exclude [binding])
  (:require-macros [hoplon.binding :as b]))

(def thread-bindings
  "Stack of binding maps like this:

      {
        my.namespace.core   {:push! (fn [] ...) :pop! (fn [] ...)}
        other.namespace.foo {:push! (fn [] ...) :pop! (fn [] ...)}
        ...

  where the keys of the map are the Javascript variables (as symbols) and
  the values are maps with :push! and :pop! keys, each associated with a
  zero arity procedure that pushes or pops the thread binding for that var."
  (atom []))

(defn push-thread-bindings
  "Pushes binding-map onto the thread-bindings stack and establishes the
  associated bindings."
  [binding-map]
  (letfn [(reducer [xs k {:keys [push!] :as v}]
            (assoc xs k (assoc v :pop! (push!))))]
    (swap! thread-bindings conj (reduce-kv reducer {} binding-map))))

(defn pop-thread-bindings
  "Pops the topmost binding map from thread-bindings stack and restores the
  associated bindings to their previous, saved values."
  []
  (let [popped (peek @thread-bindings)]
    (swap! thread-bindings pop)
    (doseq [{:keys [pop!]} (vals popped)] (pop!))))

(defn bound-fn*
  "Given a function f, returns a new function capturing the current bindings
  in its closure. When the returned function is invoked the saved bindings
  are pushed and set, f is applied to the arguments, and bindings are restored
  to their previous, saved values."
  [f]
  (let [binding-map (apply merge @thread-bindings)]
    (fn [& args]
      (push-thread-bindings binding-map)
      (try (apply f args) (finally (pop-thread-bindings))))))
