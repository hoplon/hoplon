(set-env!
  :source-paths   #{"src" "tst/src"}
  :dependencies (template [[adzerk/boot-cljs                      "2.0.0"    :scope "test"]
                           [adzerk/bootlaces                      "0.1.13"   :scope "test"]
                           [adzerk/boot-reload                    "0.5.1"    :scope "test"]
                           [adzerk/boot-test                      "1.2.0"    :scope "test"]
                           [boot-codox                            "0.10.3"   :scope "test"]
                           [lein-doo                              "0.1.7"    :scope "test"]
                           [crisptrutski/boot-cljs-test           "0.3.2"    :scope "test"]
                           [org.clojure/clojure                   ~(clojure-version)]
                           [org.clojure/clojurescript             "1.9.562"]
                           [cljsjs/jquery                         "3.2.1-0"]
                           [hoplon/javelin                        "3.9.0"]]))

(require
  '[adzerk.bootlaces          :refer :all]
  '[hoplon.boot-hoplon        :refer [hoplon ns+ prerender]]
  '[adzerk.boot-reload        :refer [reload]]
  '[adzerk.boot-cljs          :refer [cljs]]
  '[adzerk.boot-test          :refer [test]]
  '[codox.boot                :refer [codox]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(def +version+ "7.1.0-SNAPSHOT")

(bootlaces! +version+)

(deftask develop []
  (comp (watch) (target) (speak) (build-jar)))

(replace-task!
 [t test-cljs]
 (fn [& xs]
  (set-env! :source-paths #{"src" "tst/src/cljs"})
  (apply t xs)))

(deftask develop-tests []
  (comp (watch) (speak) (test-cljs)))

(task-options!
  pom    {:project     'hoplon
          :version     +version+
          :description "Hoplon web development environment."
          :url         "https://github.com/hoplon/hoplon"
          :scm         {:url "https://github.com/hoplon/hoplon"}
          :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}}
  target {:dir #{"target"}}
  codox  {:description "Hoplon web development environment."
          :name "Hoplon"
          :version +version+
          :language :clojurescript
          :source-uri "https://github.com/hoplon/hoplon/tree/{version}/{filepath}#L{line}"
          :filter-namespaces '[hoplon.core hoplon.storage-atom hoplon.svg hoplon.test]
          :source-paths   #{"src"}
          :metadata {:doc "FIXME: write docs"}})
