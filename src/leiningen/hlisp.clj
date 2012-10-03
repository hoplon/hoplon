(ns leiningen.hlisp
  (:require
    [hlisp.core :as hl]))

(defn hlisp
  "Hlisp compiler.
  
  USAGE: lein hlisp
  Compile once.
  
  USAGE: lein hlisp auto
  Watch source dirs and compile when necessary."
  ([project]
   (hl/compile-fancy (:hlisp project)))
  ([project auto] 
   (hl/watch-compile (:hlisp project))))

(comment

  {:html-src    "src/html"
   :cljs-src    "src/cljs"
   :html-out    "resources/public"
   :prelude     "src/template/prelude.cljs"
   :includes    ["src/jslib/jquery.js"]
   :cljsc-opts  {:optimizations  :advanced
                 :externs        ["src/extern/jquery.js"]}} 
  ) 
