;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon
  (:refer-clojure :exclude [subs name])
  (:require
    [clojure.walk         :as walk]
    [clojure.core.strint  :as strint]))

(create-ns 'js)
(create-ns 'tailrecursion.javelin)

(defn subs [& args] (try (apply clojure.core/subs args) (catch Throwable _)))
(defn name [& args] (try (apply clojure.core/name args) (catch Throwable _)))

(defn add-doc [docstring pair]
  (if (string? docstring) (list (first pair) docstring (last pair)) pair))

(defn do-def [docstring bindings values]
  (->> (macroexpand `(let [~bindings ~values]))
       (second)
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

(defn terpol8 [s]
  (let [parts (remove #(= "" %) (#'strint/interpolate s))]
    (if (every? string? parts) s `(str ~@parts))))

(defn flatten-expr-1 [expr sym]
  (require 'cljs.compiler)
  (require 'cljs.core)
  (require 'cljs.analyzer)
  (let [ana-spc  (var-get (resolve 'cljs.analyzer/specials))
        ana-xpn  (var-get (resolve 'cljs.analyzer/get-expander))
        ana-env  (var-get (resolve 'cljs.analyzer/empty-env))
        apply?   #(and (seq? %) (symbol? (first %)))
        special? #(contains? ana-spc (first %))
        macro?   #(ana-xpn (first %) (ana-env))
        flatten? #(and (apply? %) (not (or (macro? %) (special? %))))
        flatten  (fn [[op & args]]
                   (let [args* (map #(if (flatten? %) (gensym) %) args)
                         flat  #(when (flatten? %1) (flatten-expr-1 %1 %2))
                         prep  (mapcat identity (map flat args args*))]
                     (concat prep [sym (list* op args*)])))]
    (if-not (flatten? expr) [sym expr] (flatten expr))))

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

(defmacro loop-tpl
  "FIXME: document this"
  [& args]
  (let [[_ {:keys [bindings bind-ids reverse]} [tpl]]
        (parse-e (cons '_ args))
        [bindings items] bindings
        mksym     (fn [& _] (gensym "hl-auto-"))
        gsyms     (into {} (map (juxt identity mksym) bind-ids))
        sym*      `(str (gensym "hl-auto-"))
        id-binds  (interleave (vals gsyms) (repeat sym*))
        body      (walk/postwalk #(get gsyms % %) tpl)]
    `(loop-tpl* ~items ~reverse
       (fn [item#]
         (let [~@id-binds]
           (tailrecursion.javelin/cell-let [~bindings item#] ~body))))))

(defmacro text
  "FIXME: document this"
  [form]
  (let [i (terpol8 form)]
    (if-not (seq? i)
      `(.createTextNode js/document ~i) 
      `(let [t# (.createTextNode js/document "")]
         (tailrecursion.javelin/cell= (set! (.-nodeValue t#) ~i))
         t#))))

(defmacro flatten-expr
  "FIXME: document this"
  [expr]
  (let [sym (gensym)]
    `(let [~@(flatten-expr-1 expr sym)] ~sym)))

(defmacro head
  "FIXME: document this"
  [& forms]
  (let [[_ attr kids] (parse-e (cons '_ forms))]
    `(html-head ~(or attr {})
       (let [~'meta html-meta]
         ~kids))))

(defmacro body
  "FIXME: document this"
  [& forms]
  (let [[_ attr kids] (parse-e (cons '_ forms))
        flatten       (fn [x] `(flatten-expr ~x))]
    `(html-body ~(or attr {}) ~@(map flatten kids))))

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
