(ns leiningen.hlisp
  (require [clojure.pprint :as pprint]))

(defn hlisp [project & args]
  (pprint/pprint (:hlisp project))
  (println "Hello leiningen!!!"))
