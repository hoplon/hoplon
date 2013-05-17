(defproject tailrecursion/hoplon "0.1.0-SNAPSHOT"
  :description        "Hoplon web development environment."
  :url                "http://github.com/tailrecursion/hoplon"
  :license            {:name "Eclipse Public License"
                       :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen  true
  :manifest           {"hoplon-provides"            "hoplon"}
  :dependencies       [[org.clojure/clojure         "1.5.1"]
                       [tailrecursion/javelin       "1.0.0-SNAPSHOT"]
                       [fipp                        "0.3.0-SNAPSHOT"]
                       [digest                      "1.3.0"]
                       [criterium                   "0.3.0"]
                       [clj-tagsoup                 "0.3.0"]
                       [org.clojure/core.incubator  "0.1.2"]
                       [org.clojure/clojurescript   "0.0-1552"]])
