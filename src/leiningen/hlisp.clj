(ns leiningen.hlisp
  (:require
    [fs.core :as fs])
  (:use
    [clojure.java.io  :only [file as-file reader resource]]
    [clojure.pprint   :only [pprint]]))

(defn hlisp [project & args]
  (println (slurp (reader (resource "foo.txt")))) 
  (println "omfg"))


(comment

  (hlisp {:hlisp {:workdir "work"}})
  
  ) 
