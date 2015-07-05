;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon
  (:refer-clojure :exclude [subs name])
  (:require [clojure.walk :as walk]))

(create-ns 'js)
(create-ns 'tailrecursion.javelin)

(defn subs [& args] (try (apply clojure.core/subs args) (catch Throwable _)))
(defn name [& args] (try (apply clojure.core/name args) (catch Throwable _)))

(defn add-doc [docstring pair]
  (if (string? docstring) (list (first pair) docstring (last pair)) pair))

(defn do-def [docstring bindings values]
  (->> (macroexpand `(let [~bindings ~values]))
       (second)
       (walk/postwalk-replace
         {'clojure.lang.PersistentHashMap/create '(partial apply hash-map)})
       (partition 2)
       (map (partial add-doc docstring))
       (map #(cons 'def %))
       (list* 'do)))

(defn parse-e [[tag & [head & tail :as args]]]
  (let [kw1? (comp keyword? first)
        mkkw #(->> (partition 2 %) (take-while kw1?) (map vec))
        drkw #(->> (partition 2 2 [] %) (drop-while kw1?) (mapcat identity))]
    (cond (map?     head) [tag head tail]
          (keyword? head) [tag (into {} (mkkw args)) (drkw args)]
          :else           [tag nil args])))

(defn- ^{:from 'org.clojure/core.incubator} silent-read
  "Attempts to clojure.core/read a single form from the provided String, returning
  a vector containing the read form and a String containing the unread remainder
  of the provided String. Returns nil if no valid form can be read from the
  head of the String."
  [s]
  (try
    (let [r (-> s java.io.StringReader. java.io.PushbackReader.)]
      [(read r) (slurp r)])
    (catch Exception e))) ; this indicates an invalid form -- the head of s is just string data

(defn- ^{:from 'org.clojure/core.incubator} terpol8*
  "Yields a seq of Strings and read forms."
  ([s atom?]
   (lazy-seq
     (if-let [[form rest] (silent-read (subs s (if atom? 2 1)))]
       (cons form (terpol8* (if atom? (subs rest 1) rest)))
       (cons (subs s 0 2) (terpol8* (subs s 2))))))
  ([^String s]
   (if-let [start (->> ["~{" "~("]
                       (map #(.indexOf s ^String %))
                       (remove #(== -1 %))
                       sort
                       first)]
     (lazy-seq (cons
                 (subs s 0 start)
                 (terpol8* (subs s start) (= \{ (.charAt s (inc start))))))
     [s])))

(defn terpol8 [s]
  (let [parts (remove #(= "" %) (terpol8* s))]
    (if (every? string? parts) s `(str ~@parts))))

(defmacro def-values
  "Destructuring def, similar to scheme's define-values."
  ([bindings values]
   (do-def nil bindings values))
  ([docstring bindings values]
   (do-def docstring bindings values)))

(defmacro defelem
  "FIXME: document this"
  [name & forms]
  (let [[_ name [_ & [[bind & body]]]] (macroexpand-1 `(defn ~name ~@forms))]
    `(def ~name (fn [& args#] (let [~bind (parse-args args#)] ~@body)))))

(defn- map-bind-keys
  [form]
  (when (map? form)
    (->> form
         :keys
         (map (juxt identity #(keyword (name %))))
         (into (dissoc form :keys))
         vals
         (filter keyword?))))

(defmacro component
  "FIXME: document this"
  [[bind-attr bind-kids] & body]
  (let [attr-keys (map-bind-keys bind-attr)]
    `(fn [& args#]
       (let [[attr# kids#] (parse-args args#)]
         (-> (let [kids*# (tailrecursion.javelin/cell [])
                   attr*# (tailrecursion.javelin/cell
                            ~(zipmap attr-keys (repeat nil)))]
               (tailrecursion.javelin/cell-let
                 [~bind-attr attr*#
                  ~bind-kids (tailrecursion.javelin/cell= (flatten kids*#))]
                 (doto (do ~@body)
                   (set-appendChild! (constantly kids*#))
                   (set-removeChild! (constantly kids*#))
                   (set-setAttribute! (constantly attr*#)))))
             (apply attr# kids#))))))

(defmacro defcomponent
  "FIXME: document this"
  [name & forms]
  (let [[_ name [_ [bind & body]]] (macroexpand-1 `(defn ~name ~@forms))]
    `(def ~name (component ~bind ~@body))))

(defmacro loop-tpl
  "FIXME: document this"
  [& args]
  (let [[_ {[bindings items] :bindings} [body]] (parse-e (cons '_ args))]
    `(loop-tpl* ~items
       (fn [item#]
         (tailrecursion.javelin/cell-let [~bindings item#] ~body)))))

(defmacro with-dom
  [elem & body]
  `(when-dom ~elem (fn [] ~@body)))

(defmacro text
  "FIXME: document this"
  [form]
  (let [i (if-not (string? form) form (terpol8 form))]
    (if (string? i)
      `(.createTextNode js/document ~i)
      `(let [t# (.createTextNode js/document "")]
         (tailrecursion.javelin/cell= (set! (.-nodeValue t#) ~i))
         t#))))

(defmacro with-timeout
  "FIXME: document this"
  [msec & body]
  `(js/setTimeout (fn [] ~@body) ~msec))

(defmacro with-interval
  "FIXME: document this"
  [msec & body]
  `(js/setInterval (fn [] ~@body) ~msec))

(defmacro with-init!
  "FIXME: document this"
  [& body]
  `(add-initfn! (fn [] ~@body)))

(defmacro with-page-load
  "FIXME: document this"
  [& body]
  `(defonce page-load# (on-page-load (fn [] ~@body))))

(defmacro sexp
  [& args]
  (->> (last (parse-e (cons '_ args)))
       (mapcat #(if-not (string? %)
                  [%]
                  (read-string (str "(" % "\n)"))))))

