(ns tailrecursion.hoplon.compiler.util
  (:refer-clojure :exclude [read-string])
  (:require
   [clojure.string :as string]
   [clojure.tools.reader :as r]))

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

(defn munge-page-name [x]
  (-> (str "_" (name x)) (string/replace #"\." "_DOT_") munge))

(defn munge-page [ns]
  (let [ap "tailrecursion.hoplon.app-pages."]
    (if (symbol? ns) ns (symbol (str ap (munge-page-name ns))))))

(defn get-ns [forms-str]
  (->> forms-str clojure.core/read-string second munge-page))

(defn read-string [forms-str]
  (binding [*ns*          (get-ns forms-str)
            r/*alias-map* (get-aliases forms-str)]
    (r/read-string (str "(" forms-str "\n)"))))
