(defproject tailrecursion/lein-hoplon "0.1.0-SNAPSHOT"
  :description        "Hoplon web development compiler plugin."
  :url                "http://github.com/tailrecursion/lein-hoplon"
  :license            {:name "Eclipse Public License"
                       :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen  true
  :dependencies       [[org.clojure/clojure "1.5.1"]
                       [fipp "0.3.0-SNAPSHOT"]
                       [digest "1.3.0"]
                       [tailrecursion/hlisp-macros "1.0.0"]
                       [criterium "0.3.0"]
                       [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                       [org.clojure/clojurescript "0.0-1552"]])
