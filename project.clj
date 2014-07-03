(defproject tailrecursion/hoplon "5.10.7"
  :description  "Hoplon web development environment."
  :url          "http://github.com/tailrecursion/hoplon"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-marginalia            "0.7.1"]]
  :dependencies [[io.hoplon.vendor/jquery    "1.8.2-0"]
                 [tailrecursion/javelin      "3.3.1"]
                 [tailrecursion/castra       "2.0.0"]
                 [clj-tagsoup                "0.3.0"]
                 [org.clojure/core.incubator "0.1.2"]])
