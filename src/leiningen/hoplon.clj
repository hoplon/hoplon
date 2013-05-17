(ns leiningen.hoplon
  (:import
    [java.util.jar JarFile]
    [java.util.zip ZipFile])
  (:require
    [clojure.java.io                          :refer [file input-stream make-parents]]
    [clojure.pprint                           :refer [pprint]]
    [tailrecursion.hoplon.compiler.kahnsort   :refer [topo-sort]]
    [tailrecursion.hoplon.compiler.re-map     :refer [re-map]]
    [tailrecursion.hoplon.compiler.file       :as f]
    [tailrecursion.hoplon.compiler.core       :as hl]
    [tailrecursion.hoplon.compiler.compiler   :as hlc]
    [leiningen.core.classpath                 :as cp]))

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
  {:html-src      "src/html"
   :static-src    "src/static"
   :include-src   "src/include"
   :cljs-src      "src/cljs"
   :work-dir      ".hoplon-work-dir"
   :html-out      "resources/public"
   :outdir-out    "out"
   :pre-script    "pre-compile"
   :post-script   "post-compile"
   :pretty-print  false
   :includes      []
   :cljsc-opts    {:externs []}})

(defn process-opts [opts]
  (let [opts (deep-merge-with #(last %&) default-opts opts)
        work #(.getPath (apply file (:work-dir opts) %&))]
    (assoc opts :html-work     (work "html")
                :cljs-work     (work "cljs")
                :cljs-stage    (work "cljs-stage")
                :out-work      (work "out")
                :stage-work    (work "stage")
                :include-work  (work "include")
                :inc-dep       (work "dep" "inc")
                :lib-dep       (work "dep" "lib")
                :flib-dep      (work "dep" "flib")
                :ext-dep       (work "dep" "ext")
                :cljs-dep      (work "dep" "cljs"))))

(defn hoplon-dep-jar? [jar]
  (let [attrs (-> (.getManifest jar) (.getMainAttributes))]
    (and (-> (zipmap (mapv str (keys attrs)) (vals attrs))
           (get "hoplon-provides"))
         jar)))

(defn extract-jar! [jar entry dir filename]
  (let [dest (file dir filename)]
    (make-parents dest)
    (spit dest (slurp (.getInputStream jar entry)))))

(defn munge-name [f]
  (str "________" (format "%010d" (counter)) "_" f))

(defn process-jar! [jar opts]
  (let [dirmap    (re-map #"\.inc\.js$"   (:inc-dep opts) 
                          #"\.ext\.js$"   (:ext-dep opts) 
                          #"\.lib\.js$"   (:lib-dep opts) 
                          #"\.flib\.js$"  (:flib-dep opts) 
                          #"\.cljs$"      (:cljs-dep opts))
        entries   (enumeration-seq (.entries jar))
        names     (map #(munge-name (.getName %)) entries)
        dirs      (map dirmap names)
        depfiles  (filter second (trans [entries dirs names]))]
    (mapv (partial apply extract-jar! jar) depfiles)))

(defn process-dep! [dep opts]
  (let [f (:file (meta dep))]
    (when-let [jar (and (re-find #"\.jar$" (.getName f))
                        (hoplon-dep-jar? (JarFile. f)))]
      (process-jar! jar opts))))

(defn install-hoplon-deps! [project opts]
  (let [deps (#'cp/get-dependencies :dependencies project)] 
    (when (count deps)
      (if-let [sorted (topo-sort deps)]
        (mapv #(process-dep! % opts) sorted)
        (throw (Exception. (str "Circular dependency: " (pr-str deps))))))))

(defn start-compiler [project auto]
  (if (f/lockfile ".hoplon-lock")
    (let [opts (process-opts (or (:hoplon project) {}))]
      (hl/prepare opts)
      (install-hoplon-deps! project opts)
      (binding [hlc/*printer* (if (:pretty-print opts) pprint prn)]
        (hl/start opts :auto auto)))
    (println (str "Hoplon compiler is already running in JVM '"
                  (slurp ".hoplon-lock")
                  "'."))))
  
(defn hoplon
  "Hoplon compiler.
  
  USAGE: lein hoplon
  Compile once.
  
  USAGE: lein hoplon auto
  Watch source dirs and compile when necessary."
  ([project] (start-compiler project false))
  ([project auto] (start-compiler project true)))
