(ns hlisp.macros
  (:use
    [clojure.walk   :only [postwalk]]
    [clojure.string :only [blank? split]]) 
  (:require
    [clojure.core.strint :as strint]))

(let [add-docstring (fn [docstring pair]
                      (if (string? docstring)
                        (list (first pair) docstring (last pair))
                        pair))
      do-def-values (fn [docstring bindings values]
                      (->>
                        (macroexpand `(let [~bindings ~values]))
                        (second)
                        (partition 2)
                        (map (partial add-docstring docstring)) 
                        (map #(cons 'def %))
                        (list* 'do)))]
  (defmacro def-values
    "Destructuring def, similar to scheme's define-values."
    ([bindings values] 
     (do-def-values nil bindings values))
    ([docstring bindings values]
     (do-def-values docstring bindings values))))

(defmacro def-elem
  [param body]
  `(do (def ~param ~body) nil))

(defmacro def-elems
  [params body]
  `(do (def-values [~@(rest params)] ~body) nil))

(defmacro tpl [params body]
  `(fn [~@(rest params)] ~body))

(defmacro deftpl
  [nm params body]
  `(do (def ~nm (tpl ~params ~body)) nil))

(let [i (fn [template]
          (let [parts (remove #(= "" %) (#'strint/interpolate template))]
            (if (every? string? parts) (apply str parts) `(str ~@parts))))
      interpolate (fn [& forms]
                    (postwalk #(if (string? %) (i %) %) forms))]
  (defmacro interpolating
    [& body]
    `(do ~@(apply interpolate body))))
