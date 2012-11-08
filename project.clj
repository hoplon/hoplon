(defproject
  lein-hlisp          "0.1.0-SNAPSHOT"
  :description        "Hlisp compiler."
  :url                "http://github.com/micha/lein-hlisp"
  :license            {:name "Eclipse Public License"
                       :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen  true
  :dependencies       [[org.clojure/clojure "1.4.0"]
                       [com.cemerick/pomegranate "0.0.13"]
                       [hlisp-macros "0.1.0-SNAPSHOT"]
                       [criterium "0.3.0"]
                       [org.clojars.jmeeks/jtidy "r938"]
                       [hiccup "1.0.1"]
                       [compojure "1.0.4"]
                       [clj-tagsoup "0.3.0" :exclusions  [org.clojure/clojure]]
                       [org.clojure/clojurescript "0.0-1450"]
                       [fs "1.3.2"]])
