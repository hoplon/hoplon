(ns hoplon.boot-hoplon.kahn
  (:refer-clojure :exclude [sort])
  (:require [clojure.set :refer [difference union intersection]]))

(defn choose
  "Returns the pair [element, s'] where s' is set s with element removed."
  [s] {:pre [(not (empty? s))]}
  (let [item (first s)]
    [item (disj s item)]))

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
    (reduce #(if (% %2) % (assoc % %2 #{})) g have-incoming)))

(defn sort
  "Proposes a topological sort for directed graph g using Kahn's
   algorithm, where g is a map of nodes to sets of nodes. If g is
   cyclic, returns nil."
  ([g]
     (sort (normalize g) [] (no-incoming g)))
  ([g l s]
     (if (empty? s)
       (if (every? empty? (vals g)) l)
       (let [[n s'] (choose s)
             m (g n)
             g' (reduce #(update-in % [n] disj %2) g m)]
         (recur g' (conj l n) (union s' (intersection (no-incoming g') m)))))))
