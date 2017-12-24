;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns hoplon.core
  (:import [java.util UUID])
  (:require [clojure.walk    :as walk]
            [clojure.java.io :as io]
            [clojure.string  :as string]
            [clojure.spec.alpha :as spec]
            [javelin.core    :as j]
            [hoplon.spec]))

;; Macro Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(defn- ^{:from 'org.clojure/core.incubator} silent-read
;  "Attempts to clojure.core/read a single form from the provided String, returning
;  a vector containing the read form and a String containing the unread remainder
;  of the provided String. Returns nil if no valid form can be read from the
;  head of the String."
;  [s]
;  (try
;    (let [r (-> s java.io.StringReader. java.io.PushbackReader.)]
;      [(read r) (slurp r)]
;    (catch Exception e))) ; this indicates an invalid form -- the head of s is just string data
;
;(defn- ^{:from 'org.clojure/core.incubator} terpol8*
;  "Yields a seq of Strings and read forms."
;  ([s atom?]
;   (lazy-seq
;     (if-let [[form rest] (silent-read (subs s (if atom? 2 1)))]
;       (cons form (terpol8* (if atom? (subs rest 1) rest)))
;       (cons (subs s 0 2) (terpol8* (subs s 2))))
;  ([^String s]
;   (if-let [start (->> ["~{" "~("]
;                       (map #(.indexOf s ^String %))
;                       (remove #(== -1 %))
;                       sort
;                       first]
;     (lazy-seq (cons
;                 (subs s 0 start)
;                 (terpol8* (subs s start) (= \{ (.charAt s (inc start))))]
;     [s]))
;
;(defn terpol8 [s]
;  (let [parts (remove #(= "" %) (terpol8* s))]
;    (if (every? string? parts) s `(str ~@parts))))
;
;(defmacro fmt [^String string]
;  (let [-re #"#\{(.*?)\}"
;        fstr (clojure.string/replace string -re "%s")
;        fargs (map #(read-string (second %)) (re-seq -re string))
;    `(format ~fstr ~@fargs)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Defining Macros ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmacro elem
  "Create an anonymous custom element."
  [bind & body]
  (let [[prepost & body] (if (map? (first body)) body (conj body nil))]
    `(fn [& args#] ~(or prepost {}) (let [~bind (parse-args args#)] ~@body))))

(spec/fdef elem :args :hoplon.spec/elem :ret any?)

(defmacro defelem
  "Defines an element function.

  An element function creates a DOM Element (parent) given two arguments:

    * `attrs` - a number of key-value pairs for attributes and their values
    * `kids` - a sequence of DOM Elements to be appended/used inside

  The returned DOM Element is itself a function which can accept more
  attributes and child elements."
  [name & forms]
  (let [[_ name [_ & [fdecl]]] (macroexpand-1 `(defn ~name ~@forms))
        [docstr & [bind & body]] (if (string? (first fdecl)) fdecl (conj fdecl nil))]
    `(def ^{:doc ~docstr} ~name (elem ~bind ~@body))))

(spec/fdef defelem :args :hoplon.spec/defelem :ret any?)

(defmacro defattr
  "Defines an attribute function.

  An element attribute is a function given three arguments:

    * `elem` - the target DOM Element containing the attribute
    * `key` - the attribute keyword or symbol
    * `value` - the attribute value

  The attribute function is called whenever the value argument changes."
  [name & forms]
  `(defmethod hoplon.core/do! ~name ~@forms))

(spec/fdef defattr :args :hoplon.spec/defattr :ret any?)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Caching DOM Manipulation Macros ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmacro ^:private safe-deref [expr] `(deref (or ~expr (atom nil))))

(defmacro for-tpl
  "Template. Accepts a cell-binding and returns a cell containing a sequence of
  elements:

    (for-tpl [x xs] (span x))
  "
  [[bindings items] body]
  `(loop-tpl* ~items (fn [item#] (j/cell-let [~bindings item#] ~body))))

(spec/fdef for-tpl :args :hoplon.spec/for-tpl :ret any?)

(defmacro if-tpl
  "Template. Accepts a `predicate` cell and returns a cell containing either
  the element produced by `consequent` or `alternative`, depending on the value
  of the predicate:

    (if-tpl predicate (span \"True\") (span \"False\"))
  "
  [predicate consequent & [alternative]]
  `(let [con# (delay ~consequent)
         alt# (delay ~alternative)
         tpl# (fn [p#] (safe-deref (if p# con# alt#)))]
     ((j/formula tpl#) ~predicate)))

(spec/fdef if-tpl :args :hoplon.spec/if-tpl :ret any?)

(defmacro when-tpl
  "Template. Accepts a `predicate` cell and returns a cell containing either
  the element produced by `consequent` or nothing, depending on the value of
  the predicate:

    (when-tpl predicate (span \"Value\"))

  "
  [predicate & body]
  `(if-tpl ~predicate (do ~@body)))

(spec/fdef when-tpl :args :hoplon.spec/when-tpl :ret any?)

(defmacro cond-tpl
  "Template. Accepts a number of `clauses` cell-template pairs and returns a
  cell with the value produced by the matching clause:

    (cond-tpl
      clause-a (span \"A\")
      clause-b (span \"B\")
      :else    (span \"Default\"))
  "
  [& clauses]
  (assert (even? (count clauses)))
  (let [[conds tpls] (apply map vector (partition 2 clauses))
        syms1        (take (count conds) (repeatedly gensym))
        syms2        (take (count conds) (repeatedly gensym))]
    `(let [~@(interleave syms1 (map (fn [x] `(delay ~x)) tpls))
           tpl# (fn [~@syms2] (safe-deref (cond ~@(interleave syms2 syms1))))]
       ((j/formula tpl#) ~@conds))))

(spec/fdef cond-tpl :args :hoplon.spec/cond-tpl :ret any?)

(defmacro case-tpl
  "Template. Accepts an `expr` cell and a number of `clauses` and returns a
  cell with the value produced by the matching clause:

    (case-tpl expr
      :a (span \"A\")
      :b (span \"B\")
      (span \"Default\"))

  "
  [expr & clauses]
  (let [[cases tpls] (apply map vector (partition 2 clauses))
        default      (when (odd? (count clauses)) (last clauses))
        syms         (take (inc (count cases)) (repeatedly gensym))]
    `(let [~@(interleave syms (map (fn [x] `(delay ~x)) (conj tpls default)))
           tpl# (fn [expr#] (safe-deref (case expr# ~@(interleave cases syms) ~(last syms))))]
       ((j/formula tpl#) ~expr))))

(spec/fdef case-tpl :args :hoplon.spec/case-tpl :ret any?)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; DOM Macros ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmacro with-dom
  "Evaluates the body after elem has been inserted into the DOM."
  [elem & body]
  `(when-dom ~elem (fn [] ~@body)))

(defmacro with-timeout
  "Evaluates the body after msec milliseconds, asynchronously. Returns the
  timeout ID which can be used to cancel the operation (see js/clearTimeout)."
  [msec & body]
  `(js/setTimeout (fn [] ~@body) ~msec))

(defmacro with-interval
  "Evaluates the body every msec milliseconds, asynchronously. Returns the
  interval ID which can be used to cancel the operation (see js/clearInterval)."
  [msec & body]
  `(js/setInterval (fn [] ~@body) ~msec))

(defmacro with-init!
  "Evaluates the body after Hoplon has completed constructing the page."
  [& body]
  `(add-initfn! (fn [] ~@body)))

(defmacro with-page-load
  "Evaluates the body when the page is reloaded OR when live-reload reloads."
  [& body]
  `(defonce page-load# (on-page-load (fn [] ~@body))))

;(defmacro text
;  "Creates a DOM Text node and binds its text content to a formula created via
;  string interpolation, so the Text node updates with the formula."
;  [form]
;  (let [i (if-not (string? form) form (terpol8 form))]
;    (if (string? i)
;      `(.createTextNode js/document ~i)
;      `(j/with-let [t# (.createTextNode js/document "")]
;         (j/cell= (set! (.-nodeValue t#) ~i))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
