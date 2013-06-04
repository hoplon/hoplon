(ns tailrecursion.hoplon.compiler.deps
  (:import
    [java.util.jar JarFile]
    [java.util.zip ZipFile])
  (:require
    [clojure.java.io                          :refer [file input-stream make-parents]]
    [tailrecursion.hoplon.compiler.kahnsort   :refer [topo-sort]]
    [tailrecursion.hoplon.compiler.re-map     :refer [re-map]]
    [leiningen.core.classpath                 :as cp]))

(defn make-counter []
  (let [counter (ref 0)]
    (fn [] (dosync (alter counter inc)))))

(def counter (make-counter))

(def trans (partial apply map vector))

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

(defn install-deps! [project opts]
  (let [deps (#'cp/get-dependencies :dependencies project)] 
    (when (count deps)
      (if-let [sorted (topo-sort deps)]
        (mapv #(process-dep! % opts) sorted)
        (throw (Exception. (str "Circular dependency: " (pr-str deps))))))))

