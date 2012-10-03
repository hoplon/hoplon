(ns hlisp.watchdir
  (:use
    [clojure.java.io  :only [file]]
    [clojure.set      :only [union intersection difference]]))

(defn get-latest [dir]
  (set (mapv (fn [f] {:file f :time (.lastModified f)})
             (file-seq (file dir))))) 

(defn get-modified [before now]
  (->>
    (difference (union before now) (intersection before now))
    (group-by :file)
    (vals)
    (map (comp last (partial sort-by :time)))))

(defn file-ext [f]
  (let [fname (.getName f)
        ext   (subs fname (inc (.lastIndexOf fname ".")))]
    (when (not= ext fname) ext)))

(defn add-watch* [b f]
  (let [k (keyword `foo#)]
    (add-watch b k (fn [k v prev cur] (f prev cur)))))

(defn timer-b
  ([ms]
   (timer-b (fn [] (System/currentTimeMillis))))
  ([ms f & args] 
   (let [ret (atom nil)]
     (future
       (loop []
         (Thread/sleep ms)
         (swap! ret (constantly (apply f args)))
         (recur)))
     ret)))

(defn changes-b [b]
  (let [ret (atom nil)]
    (add-watch* b #(when (not= %1 %2) (swap! ret (constantly %2))))
    ret))

(defn map-b [f b]
  (let [ret (atom nil)]
    (add-watch* b #(swap! ret (constantly (f %1 %2))))
    ret))

(defn mapcat-b [f b]
  (let [ret (atom nil)]
    (add-watch* b (fn [_ cur] (mapv #(swap! ret (constantly (f %))) cur)))
    ret))

(defn filter-b [f b]
  (let [ret (atom nil)]
    (add-watch* b (fn [_ cur] (when (f cur) (swap! ret (constantly cur)))))
    ret))

(defn merge-b [b1 b2]
  (let [ret (atom nil)]
    (add-watch* b1 (fn [_ cur] (swap! ret (constantly cur))))
    (add-watch* b2 (fn [_ cur] (swap! ret (constantly cur))))
    ret))

(defn process-b [f b]
  (let [ret (atom nil)
        q   (java.util.concurrent.PriorityBlockingQueue.)]
    (add-watch* b (fn [_ cur] (.add q cur)))
    (future
      (loop []
        (swap! ret (constantly (f (.take q))))
        (recur)))
    ret))

(defn process-last-b [f b]
  (let [ret (atom nil)
        q   (java.util.concurrent.PriorityBlockingQueue.)]
    (add-watch* b (fn [_ cur] (.add q cur)))
    (future
      (loop []
        (let [x (.take q)]
          (when (= 0 (.size q))
            (swap! ret (constantly (f x))))) 
        (recur)))
    ret))

(defn watch-dir [dir ms]
  (->>
    (timer-b ms get-latest dir)
    (changes-b)
    (map-b get-modified)
    (mapcat-b :file)))

(defn watch-dir-ext [dir ext ms]
  (->>
    (watch-dir dir ms)
    (filter-b #(= ext (file-ext %)))))

(comment

  (def a (watch-dir "src" 100))
  (add-watch a :foo (fn [k v before now] (println now)))

  )
