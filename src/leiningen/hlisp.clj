(ns leiningen.hlisp
  (:import
    [java.util.jar JarFile]
    [java.util.zip ZipFile])
  (:use
    [clojure.set                :only [difference union intersection]]
    [clojure.java.io            :only [file input-stream make-parents]]
    [clojure.pprint             :only [pprint]])
  (:require
    [leiningen.deps               :as leindeps]
    [cemerick.pomegranate.aether  :as aether]
    [robert.hooke                 :as hooke]
    [hlisp.core                   :as hl]))

(defn without
  "Returns set s with x removed."
  [s x] (difference s #{x}))

(defn take-1
  "Returns the pair [element, s'] where s' is set s with element removed."
  [s] {:pre [(not (empty? s))]}
  (let [item (first s)]
    [item (without s item)]))

(defn no-incoming
  "Returns the set of nodes in graph g for which there are no incoming
  edges, where g is a map of nodes to sets of nodes."
  [g]
  (let [nodes (set (keys g))
        have-incoming (apply union (vals g))]
    (difference nodes have-incoming)))

(defn normalize
  "Returns g with empty outgoing edges added for nodes with incoming
  edges only.  Example: {:a #{:b}} => {:a #{:b}, :b #{}}"
  [g]
  (let [have-incoming (apply union (vals g))]
    (reduce #(if (get % %2) % (assoc % %2 #{})) g have-incoming)))

(defn kahn-sort
  "Proposes a topological sort for directed graph g using Kahn's
   algorithm, where g is a map of nodes to sets of nodes. If g is
   cyclic, returns nil."
  ([g]
     (kahn-sort (normalize g) [] (no-incoming g)))
  ([g l s]
     (if (empty? s)
       (when (every? empty? (vals g)) l)
       (let [[n s'] (take-1 s)
             m (g n)
             g' (reduce #(update-in % [n] without %2) g m)]
         (recur g' (conj l n) (union s' (intersection (no-incoming g') m)))))))

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn walk-deps! [deps f]
  (doseq [[dep subdeps] deps]
    (f dep)
    (when subdeps
      (walk-deps! subdeps f))))

(defn make-counter []
  (let [counter (ref 0)]
    (fn [] (dosync (alter counter inc)))))

(def counter (make-counter))

(def default-opts
  {:html-src    "src/html"
   :cljs-src    "src/cljs"
   :html-work   "hlwork/html"
   :cljs-work   "hlwork/cljs"
   :inc-dep     "hlwork/dep/inc"
   :ext-dep     "hlwork/dep/ext"
   :cljs-dep    "hlwork/dep/cljs"
   :html-out    "resources/public"
   :base-dir    ""
   :includes    []
   :cljsc-opts  {:optimizations :whitespace
                 :externs       []}})

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
  (str "________" (format "%010d" (counter)) "_" (.getName f)))

(defn process-jar! [jar opts]
  (let [entries (enumeration-seq (.entries jar))
        incs    (filter #(re-find #"\.inc\.js$" (.getName %)) entries)
        exts    (filter #(re-find #"\.ext\.js$" (.getName %)) entries)
        cljs    (filter #(re-find #"\.cljs$"    (.getName %)) entries)] 
    (mapv #(extract-jar! jar % (:inc-dep opts)  (munge-name %)) incs)
    (mapv #(extract-jar! jar % (:ext-dep opts)  (munge-name %)) exts)
    (mapv #(extract-jar! jar % (:cljs-dep opts) (munge-name %)) cljs)))

(defn process-dep! [dep opts]
  (let [f (:file (meta dep))]
    (when-let [jar (and (re-find #"\.jar$" (.getName f))
                        (hlisp-dep-jar? (JarFile. f)))]
      (process-jar! jar opts))))

(defn install-hlisp-deps!
  [get-dependencies dependency-key project & args]
  (let [opts (process-opts (:hlisp project)) 
        deps (get-dependencies dependency-key project)] 
    (when (count deps)
      (if-let [sorted (kahn-sort deps)]
        (mapv #(process-dep! % opts) sorted)
        (throw (Exception. "Circular dependency.")) ))))

(defn activate []
  (hooke/add-hook #'leiningen.core.classpath/get-dependencies
                  #'install-hlisp-deps!))

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
