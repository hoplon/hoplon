(ns ^:no-doc hoplon.boot-hoplon.util)

(defn get-aliases [forms-str]
  (->> forms-str
    read-string
    (filter sequential?)
    (group-by first)
    :require
    (mapcat rest)
    (filter sequential?)
    (map (juxt #(second (drop-while (partial not= :as) %)) first))
    (into {})))

;(defn get-ns [forms-str]
;  (->> forms-str read-string second munge-page))
