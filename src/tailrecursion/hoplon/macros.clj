(ns tailrecursion.hoplon.macros
  (:require
    [clojure.walk         :as walk    :refer [postwalk]]
    [clojure.string       :as string  :refer [blank? split]]
    [clojure.set          :as s       :refer [union intersection]]
    [cljs.analyzer        :as a]
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

(defmacro cljs-ns [] (name a/*cljs-ns*))

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

(let [jQuery      (symbol "js" "jQuery")
      clone       (symbol "tailrecursion.hoplon.env" "clone")
      spliced     (symbol "tailrecursion.hoplon.env" "spliced")
      cell        (symbol "tailrecursion.javelin.macros" "cell")
      deref*      (symbol "tailrecursion.javelin.core" "deref*")
      readstr     #(if-not (blank? %)
                     (let [[v & _ :as forms] (read-string (str "(" % ")"))]
                       (if (vector? v) v forms)))
      listy?      #(or (list? %)
                       (= clojure.lang.LazySeq (type %))
                       (= clojure.lang.Cons (type %))) 
      rm-attr     (fn [[tag attrs & children] attr]
                    (list* tag (dissoc attrs attr) children))
      uquote?     #(and (listy? %) (= 'clojure.core/unquote (first %)))
      sub-id      #(if (uquote? %) (list jQuery (str "#" (second %))) %)
      sub-ids     #(walk/postwalk sub-id %)
      doread      #(cond (string? %) (readstr %) (vector? %) %)
      do-1        (fn [[tag maybe-attrs & children :as form]]
                    (let [{dostr :do} (if (map? maybe-attrs) maybe-attrs {})] 
                      (if-let [exprs (sub-ids (doread dostr))]
                        `(~deref* (let [f# (~clone ~(rm-attr form :do))]
                                    (~cell (doto f# ~@exprs))))
                        form)))
      loop-1      (fn [[tag maybe-attrs & [tpl] :as form]]
                    (let [attrs?  (map? maybe-attrs)
                          tpl     (if attrs? tpl maybe-attrs)
                          attrs   (if attrs? maybe-attrs {})
                          {loopspec :loop} attrs]
                      (if-let [[looper & args] (doread loopspec)]
                        `(~looper (fn ~(vec args) ~tpl) (~tag ~(dissoc attrs :loop)))
                        form)))
      walk-1      (fn [f] #(if (listy? %) (f %) %))
      walk-all    (fn [f forms] (map #(walk/postwalk (walk-1 f) %) forms))]
  (defmacro reactive-attributes [& forms]
    `(~spliced ~@(->> forms (walk-all loop-1) (walk-all do-1)))))
