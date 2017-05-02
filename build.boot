(set-env!
  :asset-paths    #{"tst/rsc"}
  :source-paths   #{"src" "tst/src"}
  :resource-paths #{"tst/tst"}
  :dependencies '[[adzerk/boot-cljs                      "1.7.48-3" :scope "test"]
                  [adzerk/bootlaces                      "0.1.13"   :scope "test"]
                  [adzerk/boot-reload                    "0.4.11"   :scope "test"]
                  [adzerk/boot-test                      "1.1.2"    :scope "test"]
                  [clj-webdriver                         "0.7.2"    :scope "test"]
                  [tailrecursion/boot-static             "0.1.0"    :scope "test"]
                  [org.seleniumhq.selenium/selenium-java "2.53.1"   :scope "test"]
                  [boot-codox                            "0.10.1"   :scope "test"]
                  [org.clojure/clojure                   "1.7.0"]
                  [org.clojure/clojurescript             "1.7.122"]
                  [cljsjs/jquery                         "1.9.1-0"]
                  [hoplon/javelin                        "3.8.4"]])

(require
  '[adzerk.bootlaces          :refer :all]
  '[hoplon.boot-hoplon        :refer [hoplon prerender]]
  '[adzerk.boot-reload        :refer [reload]]
  '[adzerk.boot-cljs          :refer [cljs]]
  '[adzerk.boot-test          :refer [test]]
  '[tailrecursion.boot-static :refer [serve]]
  '[codox.boot                :refer [codox]])

(def +version+ "7.0.0")

(bootlaces! +version+)

(replace-task!
  [t test] (fn [& xs] (comp (hoplon) (cljs) (serve) (apply t xs))))

(deftask develop-tests []
  (comp (watch) (speak) (test)))

(deftask develop []
  (comp (watch) (target) (speak) (build-jar)))

(task-options!
  pom    {:project     'hoplon
          :version     +version+
          :description "Hoplon web development environment."
          :url         "https://github.com/hoplon/hoplon"
          :scm         {:url "https://github.com/hoplon/hoplon"}
          :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}}
  test   {:namespaces '#{hoplon.app-test}}
  serve  {:port 3020}
  target {:dir #{"target"}}
  codox  {:description "Hoplon web development environment."
          :name "Hoplon"
          :version +version+
          :language :clojurescript
          :source-uri "https://github.com/hoplon/hoplon/tree/{version}/{filepath}#L{line}"
          :filter-namespaces '[hoplon.core hoplon.storage-atom hoplon.svg hoplon.test]
          :source-paths   #{"src"}
          :metadata {:doc "FIXME: write docs"}})
