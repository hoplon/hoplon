(defproject tailrecursion/hoplon "5.6.1-SNAPSHOT"
  :description  "Hoplon web development environment."
  :url          "http://github.com/tailrecursion/hoplon"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-marginalia             "0.7.1"]]
  :dependencies [[tailrecursion/javelin       "3.1.0"]
                 [tailrecursion/castra        "1.0.1"]
                 [clj-tagsoup                 "0.3.0"]
                 [org.clojure/core.incubator  "0.1.2"]])
