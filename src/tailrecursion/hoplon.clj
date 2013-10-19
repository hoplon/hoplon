(ns tailrecursion.hoplon
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

(let [add-doc (fn [docstring pair]
                (if (string? docstring)
                  (list (first pair) docstring (last pair))
                  pair))
      do-def  (fn [docstring bindings values]
                (->>
                  (macroexpand `(let [~bindings ~values]))
                  (second)
                  (partition 2)
                  (map (partial add-doc docstring)) 
                  (map #(cons 'def %))
                  (list* 'do)))]
  (defmacro def-values
    "Destructuring def, similar to scheme's define-values."
    ([bindings values] 
     (do-def nil bindings values))
    ([docstring bindings values]
     (do-def docstring bindings values))))

(defn terpol8 [s]
  (let [parts (remove #(= "" %) (#'i/interpolate s))]
    (if (every? string? parts) s `(str ~@parts))))

(create-ns 'js)
(create-ns 'tailrecursion.javelin)

(let [jQuery      (symbol "js"                    "jQuery")
      clone'      (symbol "tailrecursion.hoplon"  "clone")
      spliced'    (symbol "tailrecursion.hoplon"  "spliced")
      cell='      (symbol "tailrecursion.javelin" "cell=")
      deref*'     (symbol "tailrecursion.javelin" "deref*")
      do!'        (symbol "tailrecursion.hoplon"  "do!")
      on!'        (symbol "tailrecursion.hoplon"  "on!")
      $text'      (symbol "tailrecursion.hoplon"  "$text")
      unq         'clojure.core/unquote
      unqs        'clojure.core/unquote-splicing
      readstr     #(if-not (blank? %)
                     (let [[v & _ :as forms] (read-string (str "(" % ")"))]
                       (if (vector? v) v forms)))
      listy?      #(or (list? %)
                       (= clojure.lang.LazySeq (type %))
                       (= clojure.lang.Cons (type %))) 
      uquote?     #(and (listy? %) (= unq (first %)) (keyword? (second %)))
      uquote-spl? #(and (listy? %) (= unqs (first %)) (keyword? (second %)))
      sub-id      #(cond (uquote-spl? %)  `(~jQuery ~(str "#" (name (second %)))) 
                         (uquote? %)      `(.val (~jQuery ~(str "#" (name (second %))))) 
                         :else            %)
      sub-ids     #(walk/postwalk sub-id %)
      doread      #(cond (string? %) (readstr %) (vector? %) %)
      hl-attr*    (fn [attrs]
                    (let [attrs (assoc attrs :do (or (doread (:do attrs)) []))
                          k*    #(-> % name (subs 3) keyword)]
                      (loop [x attrs, [[k v] & more] (seq attrs)]
                        (let [add #(-> (dissoc x k)
                                     (update-in [:do] conj `(~% ~(k* k) ~@(doread v))))]
                          (if-not k x (case (-> k name (subs 0 3))
                                        "do-" (recur (add do!') more)
                                        "on-" (recur (add on!') more)
                                        (recur x more)))))))
      hl-attr     #(let [x (hl-attr* %)] (if (empty? (:do x)) (dissoc x :do) x)) 
      do-1        (fn [[tag maybe-attrs & children :as form]]
                    (let [attrs?    (map? maybe-attrs)
                          attrs     (when attrs? (hl-attr maybe-attrs))
                          kids      (if attrs? children (cons maybe-attrs children))
                          form      `(~tag ~@(when attrs [(dissoc attrs :do)]) ~@kids)
                          dostr     (:do attrs)]
                      (if-let [exprs (seq (sub-ids (doread dostr)))]
                        (let [{ons "on!" dos "do!"} (group-by (comp name first) exprs)]
                          `(let [f# (~clone' ~form)]
                             (doto f# ~@ons)
                             (~deref*' (~cell=' (doto f# ~@dos)))))
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
      do-text     (fn [x]
                    (let [i (terpol8 (if (listy? x) (second x) x))
                          k :tailrecursion.hoplon/on-create]
                      (if-not (listy? i) x `(with-meta
                                              (~$text' "")
                                              {~k (fn [n#] (~cell=' (set! (.-nodeValue n#) ~i)))}))))
      text?       #(or (string? %) (and (listy? %) (= '$text (first %))))
      walk-text   (fn w [x] (cond (text? x) (do-text x) (listy? x) (map w x) :else x))
      walk-1      (fn [f] #(if (listy? %) (f %) %))
      walk-all    (fn [f forms] (map #(walk/postwalk (walk-1 f) %) forms))]
  (defmacro reactive-attributes [& forms]
    `(~spliced' ~@(->> forms (walk-all loop-1) (walk-all do-1))))
  (defmacro with-frp [& forms]
    `(~spliced' ~@(->> forms walk-text (walk-all loop-1) (walk-all do-1)))))
