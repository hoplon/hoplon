(ns tailrecursion.hoplon.macros
  (:refer-clojure :exclude [subs name read-string])
  (:require
    [clojure.walk         :as walk    :refer [postwalk]]
    [clojure.string       :as string  :refer [blank? split]]
    [clojure.set          :as s       :refer [union intersection]]
    [cljs.analyzer        :as a]
    [clojure.core.strint  :as i]))

(defn subs [& args] (try (apply clojure.core/subs args) (catch Throwable _)))
(defn name [& args] (try (apply clojure.core/name args) (catch Throwable _)))
(defn read-string [s] (when (and (string? s) (not (blank? s))) (clojure.core/read-string s)))

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
(create-ns 'tailrecursion.hoplon.reactive)
(create-ns 'tailrecursion.javelin.macros)
(create-ns 'tailrecursion.javelin.core)

(let [jQuery      (symbol "js" "jQuery")
      clone       (symbol "tailrecursion.hoplon.env" "clone")
      spliced     (symbol "tailrecursion.hoplon.env" "spliced")
      cell        (symbol "tailrecursion.javelin.macros" "cell")
      deref*      (symbol "tailrecursion.javelin.core" "deref*")
      do!         (symbol "tailrecursion.hoplon.reactive" "do!")
      on!         (symbol "tailrecursion.hoplon.reactive" "on!")
      readstr     #(if-not (blank? %)
                     (let [[v & _ :as forms] (read-string (str "(" % ")"))]
                       (if (vector? v) v forms)))
      listy?      #(or (list? %)
                       (= clojure.lang.LazySeq (type %))
                       (= clojure.lang.Cons (type %))) 
      uquote?     #(and (listy? %) (= 'clojure.core/unquote (first %)))
      uquote-spl? #(and (listy? %) (= 'clojure.core/unquote-splicing (first %)))
      sub-id      #(cond (uquote-spl? %)  `(~jQuery ~(str "#" (second %))) 
                         (uquote? %)      `(.val (~jQuery ~(str "#" (second %)))) 
                         :else            %)
      sub-ids     #(walk/postwalk sub-id %)
      doread      #(cond (string? %) (readstr %) (vector? %) %)
      hl-attr*    (fn [attrs]
                    (let [attrs (assoc attrs :do (or (doread (:do attrs)) []))
                          k*    #(-> % name (subs 3) keyword)]
                      (loop [x attrs, [[k v] & more] (seq attrs)]
                        (let [add-do #(-> x (dissoc k) (update-in [:do] conj `(~% ~(k* k) ~@(doread v))))]
                          (if-not k x (case (-> k name (subs 0 3))
                                        "do-" (recur (add-do do!) more)
                                        "on-" (recur (add-do on!) more)
                                        (recur x more)))))))
      hl-attr     #(let [x (hl-attr* %)] (if (empty? (:do x)) (dissoc x :do) x)) 
      do-1        (fn [[tag maybe-attrs & children :as form]]
                    (let [attrs?    (map? maybe-attrs)
                          attrs     (when attrs? (hl-attr maybe-attrs))
                          kids      (if attrs? children (cons maybe-attrs children))
                          form      `(~tag ~@(when attrs [(dissoc attrs :do)]) ~@kids)
                          dostr     (:do attrs)]
                      (if-let [exprs (sub-ids (doread dostr))]
                        `(~deref* (let [f# (~clone ~form)]
                                    (~cell (doto f# ~@exprs))))
                        form)))
      loop-1      (fn [[tag maybe-attrs & [tpl] :as form]]
                    (let [
                          attrs?    (map? maybe-attrs)
                          tpl       (if attrs? tpl maybe-attrs)
                          attrs     (if attrs? maybe-attrs {})
                          container `(~tag ~(dissoc attrs :loop))
                          {loopspec :loop} attrs]
                      (if-let [[looper & args] (doread loopspec)]
                        `(~looper (fn ~(vec args) ~tpl) ~container)
                        form)))
      walk-1      (fn [f] #(if (listy? %) (f %) %))
      walk-all    (fn [f forms] (map #(walk/postwalk (walk-1 f) %) forms))]
  (defmacro reactive-attributes [& forms]
    `(~spliced ~@(->> forms (walk-all loop-1) (walk-all do-1))))
  (defmacro with-frp [& forms]
    `(~spliced ~@(->> forms (walk-all loop-1) (walk-all do-1)))))
