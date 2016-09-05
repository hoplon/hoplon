(ns hoplon.binding
  (:refer-clojure :exclude [binding bound-fn])
  (:require [clojure.core :as clj]
            [cljs.analyzer :as a]
            [cljs.compiler :as c]))

(defmethod a/error-message :bind/export
  [warning-type info]
  (str (:name info) " not declared ^:export"))

(defn- confirm-export [env var]
  (when-not (or (:export var) (= false (:bind/export a/*cljs-warnings*)))
    (clj/binding [a/*cljs-warnings* (assoc a/*cljs-warnings* :bind/export true)]
      (a/warning :bind/export env var))))

(defmacro binding
  "Like clojure.core/binding, but can be used with bound-fn. The bindings
  should not be ^:dynamic -- they must be ^:export instead."
  [bindings & body]
  (let [env           (assoc &env :ns (a/get-namespace a/*cljs-ns*))
        value-exprs   (take-nth 2 (rest bindings))
        vars-to-bind  (->> (take-nth 2 bindings)
                           (map (partial a/resolve-existing-var env)))
        js-vars       (map c/munge (map :name vars-to-bind))
        restore-syms  (take (count vars-to-bind) (repeatedly gensym))
        set-syms      (take (count vars-to-bind) (repeatedly gensym))
        setfn         (fn [x y] `(fn [] (set! ~x ~y)))
        push-pop      (fn [x y z] {:push! (setfn x y) :pop! (setfn x z)})
        thunkmaps     (map push-pop js-vars set-syms restore-syms)]
    (doseq [v vars-to-bind] (confirm-export env v))
    `(let [~@(interleave set-syms value-exprs)
           ~@(interleave restore-syms js-vars)]
       (hoplon.binding/push-thread-bindings ~(zipmap (map str js-vars) thunkmaps))
       (try ~@body (finally (hoplon.binding/pop-thread-bindings))))))

(defmacro bound-fn
  "Creates a function, capturing the dynamic bindings in place. When the
  function is applied the saved bindings are set before evaluating the body
  and restored after. See clojure.core/bound-fn."
  [args & body]
  `(hoplon.binding/bound-fn* (fn [~@args] ~@body)))
