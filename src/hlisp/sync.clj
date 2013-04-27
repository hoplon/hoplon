(ns hlisp.sync
  (:require
    [digest           :as d]
    [clojure.data     :as data]
    [clojure.java.io  :refer [copy file make-parents reader resource]]
    [clojure.set      :refer [union intersection difference]])
  (:refer-clojure :exclude [sync]))

(defn relative-to
  [base f]
  (.relativize (.toURI base) (.toURI f)))

(defn select-keys-by [m pred?]
  (select-keys m (filter pred? (keys m))))

(defn dir-set 
  ([dir] 
   (let [info (juxt #(relative-to dir %) #(.lastModified %))
         mapf #(zipmap [:dir :abs :rel :mod] (list* dir % (info %)))]
     (set (mapv mapf (filter #(.isFile %) (file-seq dir))))))
  ([dir1 dir2 & dirs]
   (reduce union (map dir-set (list* dir1 dir2 dirs)))))

(defn dir-map
  [& dirs]
  (->>
    (apply dir-set (mapv file dirs))
    (mapv #(vector (.getPath (:rel %)) %))
    (into {})))

(defn dir-map-ext
  [exts & dirs]
  (let [ext  #(let [f (.getName (file %))] (subs f (.lastIndexOf f ".")))
        ext? #(contains? exts (ext %))]
    (select-keys-by (apply dir-map dirs) ext?)))

(defn copy-with-lastmod
  [src-file dst-file]
  (make-parents dst-file)
  (copy src-file dst-file)
  (.setLastModified dst-file (.lastModified src-file)))

(defn what-changed
  ([dst-map src-map] (what-changed dst-map src-map :time))
  ([dst-map src-map algo] 
   (let [[created deleted modified]
         (data/diff (set (keys src-map)) (set (keys dst-map)))
         algos {:hash #(not= (d/md5 (:abs (src-map %)))
                             (d/md5 (:abs (dst-map %)))) 
                :time #(< (:mod (dst-map %)) (:mod (src-map %)))} 
         modified (set (filter (algos algo) modified))]
     [(union created modified) deleted])))

(defn diff
  [algo dst src & srcs]
  (let [d (dir-map (file dst))
        s (apply dir-map (mapv file (cons src srcs)))
        [to-cp to-rm] (what-changed d s algo)
        cp (map #(vector :cp (:abs (s %)) (file dst %)) to-cp) 
        rm (map #(vector :rm (file dst %)) to-rm)]
    (concat cp rm)))

(defn sync
  [dst src & srcs]
  (let [op {:cp #(copy-with-lastmod (nth % 1) (nth % 2))
            :rm #(.delete (nth % 1))}]
    (doall (map #((op (first %)) %) (apply diff :time dst src srcs)))))

(defn sync-hash
  [dst src & srcs]
  (let [op {:cp #(copy-with-lastmod (nth % 1) (nth % 2))
            :rm #(.delete (nth % 1))}]
    (doall (map #((op (first %)) %) (apply diff :hash dst src srcs)))))
