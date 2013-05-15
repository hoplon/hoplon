(ns hlisp.macros
  (:require
    [clojure.walk         :as walk    :refer [postwalk]]
    [clojure.string       :as string  :refer [blank? split]]
    [clojure.set          :as s       :refer [union intersection]]
    [clojure.core.strint  :as i]))

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
          (let [parts (remove #(= "" %) (#'i/interpolate template))]
            (if (every? string? parts) (apply str parts) `(str ~@parts))))
      interpolate (fn [& forms]
                    (postwalk #(if (string? %) (i %) %) forms))]
  (defmacro interpolating
    [& body]
    `(do ~@(apply interpolate body))))

(create-ns 'js)
(create-ns 'tailrecursion.hoplon.env)
(create-ns 'tailrecursion.javelin.macros)
(create-ns 'tailrecursion.javelin.core)

(let [jQuery  (symbol "js" "jQuery")
      clone   (symbol "tailrecursion.hoplon.env" "clone")
      cell    (symbol "tailrecursion.javelin.macros" "cell")
      deref*  (symbol "tailrecursion.javelin.core" "deref*")
      listy?  (fn [form]
                (or (list? form)
                    (= clojure.lang.LazySeq (type form))
                    (= clojure.lang.Cons (type form)))) 
      rm-attr (fn [[tag attrs & children] attr]
                (list* tag (dissoc attrs attr) children))
      sub-ids (fn [form]
                (walk/postwalk
                  #(if (and (listy? %) (= 'clojure.core/unquote (first %)))
                     (list jQuery (apply str ["#" (second %)])) 
                     %)
                  form))
      do-1    (fn [[tag maybe-attrs & children :as form]]
                (let [{dostr :do} (if (map? maybe-attrs) maybe-attrs {})
                      exprs       (if (seq dostr)
                                    (sub-ids (read-string (str "(" dostr ")"))))]
                  (if exprs
                    `(~deref*
                       (let [f# (~clone ~(rm-attr form :do))]
                         (~cell (doto f# ~@exprs))))
                    form)))]
  (defmacro reactive-attributes [form]
    (walk/postwalk #(if (listy? %) (do-1 %) %) form)))
