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
      
(defmacro def-values
  "Destructuring def, similar to scheme's define-values."
  ([bindings values] 
   (do-def nil bindings values))
  ([docstring bindings values]
   (do-def docstring bindings values)))

(create-ns 'js)
(create-ns 'tailrecursion.javelin)

(defn parse-e [[tag & [head & tail :as args]]]
  (let [kw1? (comp keyword? first)
        mkkw #(->> (partition 2 %) (take-while kw1?) (map vec))
        drkw #(->> (partition 2 2 [] %) (drop-while kw1?) (mapcat identity))]
    (cond (map?     head) [tag head tail]
          (keyword? head) [tag (into {} (mkkw args)) (drkw args)]
          :else           [tag nil args])))

(defmacro defelem [sym bindings & body]
  `(defn ~sym [& args#]
     (let [~bindings (parse-args args#)]
       ~@body)))

(defmacro loop-tpl [& args]
  (let [[_ {:keys [bindings size bind-ids done reverse]
            :or {size 0 ids []}} [tpl]]
          (parse-e (cons '_ args))
        [bindings things] bindings
        mksym     (fn [& _] (gensym "hl-auto-"))
        gsyms     (into {} (map (juxt identity mksym) bind-ids))
        sym*      `(str (gensym "hl-auto-"))
        id-binds  (interleave (vals gsyms) (repeat sym*))
        body      (walk/postwalk #(get gsyms % %) tpl)]
    `(let [things#  ((tailrecursion.javelin/lift pad-seq) ~size ~things)
           frag#    (.createDocumentFragment js/document)
           dummy#   (.createElement js/document "SPAN")]
       (if (and ~done @~done) (reset! ~done false))
       (add-initfn!
         (fn []
           (tailrecursion.javelin/cell-doseq [~bindings things#]
             (let [~@id-binds]
               (timeout
                 #(if-not ~reverse
                    (.appendChild frag# ~body)
                    (.insertBefore frag# ~body (.-firstChild frag#)))))) 
           (timeout #(.replaceChild (.-parentNode dummy#) frag# dummy#))
           (timeout #(if ~done (reset! ~done true)))))
       dummy#)))

(defn terpol8 [s]
  (let [parts (remove #(= "" %) (#'strint/interpolate s))]
    (if (every? string? parts) s `(str ~@parts))))

(defmacro text [form]
  (let [i (terpol8 form)]
    (if-not (seq? i)
      `(.createTextNode js/document ~i) 
      `(let [t# (.createTextNode js/document "")]
         (tailrecursion.javelin/cell= (set! (.-nodeValue t#) ~i))
         t#))))

(defn flatten-expr-1 [expr sym]
  (require 'cljs.compiler)
  (require 'cljs.core)
  (require 'cljs.analyzer)
  (let [ana-specials     (var-get (resolve 'cljs.analyzer/specials))
        ana-get-expander (var-get (resolve 'cljs.analyzer/get-expander))
        ana-empty-env    (var-get (resolve 'cljs.analyzer/empty-env))
        apply?    #(and (seq? %) (symbol? (first %)))
        special?  #(contains? ana-specials (first %))
        macro?    #(ana-get-expander (first %) (ana-empty-env))
        flatten?  #(and (apply? %) (not (or (macro? %) (special? %))))
        flatten   (fn [[op & args]]
                    (let [args*  (map #(if (flatten? %) (gensym) %) args)
                          flat   #(when (flatten? %1) (flatten-expr-1 %1 %2))
                          prep   (mapcat identity (map flat args args*))]
                      (concat prep [sym (list* op args*)])))]
    (if-not (flatten? expr) [sym expr] (flatten expr))))

(defmacro flatten-expr [expr]
  (let [sym (gensym)]
    `(let [~@(flatten-expr-1 expr sym)] ~sym)))
