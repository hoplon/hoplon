(defproject tailrecursion/hoplon "6.0.0-SNAPSHOT"
            :description "Hoplon web development environment."
            :url "https://github.com/tailrecursion/hoplon"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :plugins [[lein-marginalia "0.8.0"]]
            :dependencies [[cljsjs/jquery "1.8.2-2"]
                           [org.clojure/tools.reader "0.8.13"]
                           [tailrecursion/javelin "3.7.2"]
                           #_[tailrecursion/castra "2.2.2"]
                           [clj-tagsoup "0.3.0"]
                           [org.clojure/core.incubator "0.1.3"]
                           [org.clojure/clojurescript "0.0-2727"]
                           [boot/core "2.0.0-rc8" :scope "provided"]])
