(ns hoplon.boot-hoplon.util
  (:refer-clojure :exclude [read-string])
  (:require
   [clojure.string :as string]))

(defn get-aliases [forms-str]
  (->> forms-str
    clojure.core/read-string
    (filter sequential?)
    (group-by first)
    :require
    (mapcat rest)
    (filter sequential?)
    (map (juxt #(second (drop-while (partial not= :as) %)) first))
    (into {})))

;(defn get-ns [forms-str]
;  (->> forms-str clojure.core/read-string second munge-page))
