(ns tailrecursion.hoplon.util
  (:require
    [cljs.reader      :refer [read-string]]
    [clojure.string   :as string]))

(defn safe-name
  [x]
  (try (name x) (catch js/Error e)))

(let [qcache (atom ::none)]
  (defn query
    [& [k & _ :as args]]
    (if-not (= ::none @qcache)
      (if k (@qcache (safe-name k)) @qcache)
      (let [s (-> js/window .-location .-search)]
        (if (not (string/blank? s))
          (let [v (-> s (string/replace #"^\?" "") (string/split #"[&]"))
                m (->> v
                    (mapv #(string/split % #"[=]"))
                    (mapv #(mapv js/decodeURIComponent %))
                    (remove #(or (= [""] %) (= 0 (count %)) (< 2 (count %))))
                    (mapv #(if (< (count %) 2) (conj % "") %))
                    (into {}))]
            (reset! qcache m)
            (apply query args)))))))

(defn pluralize
  [word count]
  (str word (when (not= 1 count) "s")))

(defn capitalize
  [s]
  (when (string? s)
    (string/capitalize s)))

(defn str-partition
  [re s]
  (->> s
    (partition-by #(when (re-matches re %) true))
    (mapv (partial apply str))))

(defn capitalize-name
  [s]
  (->> s
    (str-partition #"[a-zA-Z]")
    (map capitalize)
    (apply str)))

(defn format-date
  [date-str]
  (let [[y m d] (mapv js/Number (string/split date-str #"-")) 
        months  ["January" "February" "March" "April" "May" "June" "July"
                 "August" "September" "October" "November" "December"]
        i       (dec m)]
    (if (< i 0)
      "<< Sorry, there was an error computing the date. >>"
      (str (nth months (dec m)) " " d ", " y))))
