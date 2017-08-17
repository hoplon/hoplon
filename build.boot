(set-env!
  :asset-paths    #{"tst/rsc"}
  :source-paths   #{"src" "tst/src"}
  :resource-paths #{"tst/tst"}
  :dependencies (template [[adzerk/boot-cljs                      "2.0.0" :scope "test"]
                           [adzerk/bootlaces                      "0.1.13"   :scope "test"]
                           [adzerk/boot-reload                    "0.5.1"   :scope "test"]
                           [adzerk/boot-test                      "1.2.0"    :scope "test"]
                           [lein-doo                              "0.1.7"    :scope "test"]
                           [crisptrutski/boot-cljs-test           "0.3.0"    :scope "test"]
                           [clj-webdriver                         "0.7.2"    :scope "test"]
                           [tailrecursion/boot-static             "0.1.0"    :scope "test"]
                           [org.seleniumhq.selenium/selenium-java "3.4.0"   :scope "test"]
                           [com.codeborne/phantomjsdriver         "1.4.3"    :scope "test" :exclusions [org.seleniumhq.selenium/selenium-java]]
                           [boot-codox                            "0.10.3"   :scope "test"]
                           [org.clojure/clojure                   ~(clojure-version)]
                           [org.clojure/clojurescript             "1.9.655"]
                           [cljsjs/jquery                         "1.9.1-0"]
                           [hoplon/javelin                        "3.9.0"]]))

(require
  '[adzerk.bootlaces          :refer :all]
  '[hoplon.boot-hoplon        :refer [hoplon ns+ prerender]]
  '[adzerk.boot-reload        :refer [reload]]
  '[adzerk.boot-cljs          :refer [cljs]]
  '[adzerk.boot-test          :refer [test]]
  '[tailrecursion.boot-static :refer [serve]]
  '[codox.boot                :refer [codox]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(def +version+ "7.0.3")

(bootlaces! +version+)

(replace-task!
  [t test] (fn [& xs] (comp (hoplon) (ns+) (cljs) (serve) (apply t xs))))

(deftask develop-tests []
  (comp (watch) (speak) (test)))

(deftask develop []
  (comp (watch) (target) (speak) (build-jar)))

(replace-task!
 [t test-cljs]
 (fn [& xs]
  (set-env! :source-paths #{"src" "tst/src/cljs"})
  (apply t xs)))

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
