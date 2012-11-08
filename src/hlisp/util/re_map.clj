(ns hlisp.util.re-map)

(defn re-map
  "re-vals is an even number of regexes/values.  Returns a map-like
  thing that attempts to sequentially match the string and return the
  appropriate value."
  [& re-vals]
  {:pre [(even? (count re-vals))]}
  (reify
    clojure.lang.ILookup
    (valAt [this o] (get this o nil))
    (valAt [_ o not-found]
      (if-let [s (seq (->> (partition 2 re-vals)
                        (map (fn [[k v]]
                               (if (re-find k o) v ::not-found)))
                        (drop-while (partial = ::not-found))))]
        (first s)
        not-found))
    clojure.lang.IFn
    (invoke [this o] (get this o))
    (invoke [this o not-found] (get this o not-found))))

(comment
  (group-by (re-map #"a" 1 #"b" 2) ["a" "b" "ab" "ba"])
  ;{1 ["a" "ab" "ba"], 2 ["b"]}

  (group-by (re-map #"b" 1 #"a" 2) ["a" "b" "ab" "ba"])
  ;{2 ["a"], 1 ["b" "ab" "ba"]}

  (get (re-map #"a" nil) "b" :nf)
  ;:nf

  (get (re-map #"a" nil) "a" :nf)
  ;nil

  )
