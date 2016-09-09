(ns hoplon.binding
  (:refer-clojure :exclude [binding bound-fn])
  (:require [clojure.core :as clj]
            [cljs.analyzer :as a]))

(defmacro binding
  "See clojure.core/binding."
  [bindings & body]
  (let [env           (assoc &env :ns (a/get-namespace a/*cljs-ns*))
        value-exprs   (take-nth 2 (rest bindings))
        bind-syms     (->> (take-nth 2 bindings)
                           (map #(:name (a/resolve-existing-var env %))))
        bind-syms'    (map (partial list 'quote) bind-syms)
        set-syms      (take (count bind-syms) (repeatedly gensym))
        setfn         (fn [x y]
                        {:push! `(fn []
                                   (let [z# ~x]
                                     (set! ~x ~y)
                                     (fn [] (set! ~x z#))))})
        thunkmaps     (map setfn bind-syms set-syms)]
    (a/confirm-bindings env bind-syms)
    `(let [~@(interleave set-syms value-exprs)]
       (hoplon.binding/push-thread-bindings ~(zipmap bind-syms' thunkmaps))
       (try ~@body (finally (hoplon.binding/pop-thread-bindings))))))

(defmacro bound-fn
  "See clojure.core/bound-fn."
  [args & body]
  `(hoplon.binding/bound-fn* (fn [~@args] ~@body)))
