(ns leiningen.hlisp
  (:require
    [hlisp.core :as hl]))

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(def default-opts
  {:html-src    "src/html"
   :cljs-src    "src/cljs"
   :html-work   "hlwork/html"
   :cljs-work   "hlwork/cljs"
   :html-out    "resources/public"
   :base-dir    ""
   :includes    []
   :cljsc-opts  {:optimizations :whitespace
                 :externs       []}})

(defn process-opts [opts]
  (deep-merge-with #(last %&) default-opts opts))

(defn hlisp
  "Hlisp compiler.
  
  USAGE: lein hlisp
  Compile once.
  
  USAGE: lein hlisp auto
  Watch source dirs and compile when necessary."
  ([project]
   (hl/compile-fancy (process-opts (:hlisp project))))
  ([project auto] 
   (hl/watch-compile (process-opts (:hlisp project)))))
