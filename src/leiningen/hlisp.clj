(ns leiningen.hlisp
  (:import
    [java.util.jar JarFile]
    [java.util.zip ZipFile])
  (:use
    [hlisp.util.kahnsort        :only [topo-sort]]
    [hlisp.util.re-map          :only [re-map]]
    [clojure.java.io            :only [file input-stream make-parents]]
    [clojure.pprint             :only [pprint]])
  (:require
    [leiningen.core.classpath     :as cp]
    [cemerick.pomegranate.aether  :as aether]
    [robert.hooke                 :as hooke]
    [hlisp.core                   :as hl]))

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn make-counter []
  (let [counter (ref 0)]
    (fn [] (dosync (alter counter inc)))))

(def counter (make-counter))

(def trans (partial apply map vector))

(def default-opts
  {:html-src    "src/html"
   :html-static "src/static"
   :cljs-src    "src/cljs"
   :html-work   "hlwork/html"
   :cljs-work   "hlwork/cljs"
   :out-work    "hlwork/out"
   :inc-dep     "hlwork/dep/inc"
   :ext-dep     "hlwork/dep/ext"
   :cljs-dep    "hlwork/dep/cljs"
   :html-out    "resources/public"
   :outdir-out  nil
   :base-dir    ""
   :pre-script  "pre-compile"
   :post-script "post-compile"
   :includes    []
   :cljsc-opts  {:externs []}})

(defn process-opts [opts]
  (deep-merge-with #(last %&) default-opts opts))

(defn hlisp-dep-jar? [jar]
  (let [attrs (-> (.getManifest jar) (.getMainAttributes))]
    (and (-> (zipmap (mapv str (keys attrs)) (vals attrs))
           (get "hlisp-provides"))
         jar)))

(defn extract-jar! [jar entry dir filename]
  (let [dest (file dir filename)]
    (make-parents dest)
    (spit dest (slurp (.getInputStream jar entry)))))

(defn munge-name [f]
  (str "________" (format "%010d" (counter)) "_" f))

(defn process-jar! [jar opts]
  (let [dirmap    (re-map #"\.inc\.js$" (:inc-dep opts) 
                          #"\.ext\.js$" (:ext-dep opts) 
                          #"\.cljs$"    (:cljs-dep opts))
        entries   (enumeration-seq (.entries jar))
        names     (map #(munge-name (.getName %)) entries)
        dirs      (map dirmap names)
        depfiles  (filter second (trans [entries dirs names]))]
    (mapv (partial apply extract-jar! jar) depfiles)))

(defn process-dep! [dep opts]
  (let [f (:file (meta dep))]
    (when-let [jar (and (re-find #"\.jar$" (.getName f))
                        (hlisp-dep-jar? (JarFile. f)))]
      (process-jar! jar opts))))

(defn install-hlisp-deps! [project]
  (let [opts (process-opts (:hlisp project)) 
        deps (#'cp/get-dependencies :dependencies project)] 
    (when (count deps)
      (if-let [sorted (topo-sort deps)]
        (mapv #(process-dep! % opts) sorted)
        (throw (Exception. (str "Circular dependency: " (pr-str deps))))))))

(defn hlisp
  "Hlisp compiler.
  
  USAGE: lein hlisp
  Compile once.
  
  USAGE: lein hlisp auto
  Watch source dirs and compile when necessary."
  ([project]
   (install-hlisp-deps! project)
   (hl/compile-fancy (process-opts (:hlisp project))))
  ([project auto] 
   (install-hlisp-deps! project)
   (hl/watch-compile (process-opts (:hlisp project)))))
