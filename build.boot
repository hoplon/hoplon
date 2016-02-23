(set-env!
  :source-paths #{"src" "test/tests"}
  :resource-paths #{"test/src"}
  ;; using the sonatype repo is sometimes useful when testing Clojurescript
  ;; versions that not yet propagated to Clojars
  ;; :repositories #(conj % '["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"}])
  :dependencies '[[org.clojure/clojure       "1.7.0" :scope "provided"]
                  [org.clojure/clojurescript "1.7.228" :scope "provided"]
                  [adzerk/bootlaces          "0.1.10"]
                  [cljsjs/jquery             "1.9.1-0"]
                  [hoplon/javelin            "3.8.4"]
                  [adzerk/boot-cljs          "1.7.48-3"]
                  [hoplon/boot-hoplon "0.1.5"]
                  [adzerk/boot-test "1.1.1" :scope "test"]
                  [org.seleniumhq.selenium/selenium-java "2.48.2"]
                  [clj-webdriver "0.7.2"]
                  [pandeiro/boot-http        "0.7.0"]])

(require '[adzerk.bootlaces :refer :all]
         '[hoplon.core :as hoplon]
         '[hoplon.boot-hoplon :refer [hoplon prerender]]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-test :refer :all]
         '[pandeiro.boot-http :refer :all]
         )

(def +version+ "6.0.0-alpha13")

(bootlaces! +version+)

(task-options!
  pom  {:project     'hoplon
        :version     +version+
        :description "Hoplon web development environment."
        :url         "https://github.com/hoplon/hoplon"
        :scm         {:url "https://github.com/hoplon/hoplon"}
        :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask webdriver-tests
  "Run all Selenium + Firefox tests"
  []
  (let [target-dir "target"]
    (comp
      (hoplon)
      (cljs)
      (prerender)
      (target :dir #{target-dir})
      (serve :dir target-dir)
      (test))))
