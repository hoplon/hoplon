(set-env!
  :resource-paths   #{"src"}
  :dependencies (template [[adzerk/boot-cljs                      "2.1.4"    :scope "test"]
                           [adzerk/boot-reload                    "0.5.1"    :scope "test"]
                           [boot-codox                            "0.10.3"   :scope "test"]
                           [lein-doo                              "0.1.8"    :scope "test"]
                           [crisptrutski/boot-cljs-test           "0.3.4"    :scope "test"]
                           [degree9/boot-semver                   "1.7.0"    :scope "test"]
                           [tolitius/boot-check                   "0.1.6"    :scope "test"]
                           [org.clojure/clojure                   ~(clojure-version)]
                           [org.clojure/clojurescript             "1.9.946"]
                           [org.clojure/test.check                "0.9.0"]
                           [cljsjs/jquery                         "3.2.1-0"]
                           [hoplon/javelin                        "3.9.0"]
                           [clj-tagsoup/clj-tagsoup "0.3.0"]]))

;; External Tasks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(require
  '[adzerk.boot-reload        :refer [reload]]
  '[adzerk.boot-cljs          :refer [cljs]]
  '[codox.boot                :refer [codox]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]]
  '[degree9.boot-semver       :refer :all]
  '[tolitius.boot-check       :as check])

(task-options!
  pom    {:project     'hoplon
          :description "Hoplon web development environment."
          :url         "https://github.com/hoplon/hoplon"
          :scm         {:url "https://github.com/hoplon/hoplon"}
          :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}}
  target {:dir #{"target"}}
  codox  {:description "Hoplon web development environment."
          :name "Hoplon"
          :language :clojurescript
          :source-uri "https://github.com/hoplon/hoplon/tree/{version}/{filepath}#L{line}"
          :filter-namespaces '[hoplon.core hoplon.storage-atom hoplon.svg hoplon.test]
          :source-paths   #{"src"}
          :metadata {:doc "FIXME: write docs"}})
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Project Tasks ;;;;;;;;;;;;;;;;::;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def test-cljs-options {:process-shim false})

(replace-task!
 [t test-cljs]
 (fn [& xs]
  (merge-env! :source-paths #{"tst/src/cljs"})
  (apply t :cljs-opts test-cljs-options xs)))

(deftask develop []
  (comp
    (version :develop true :minor 'inc :patch 'zero :pre-release 'snapshot)
    (watch) (target) (build-jar) (speak)))

(deftask develop-tests []
 (merge-env! :source-paths #{"tst/src/cljs"})
 (comp
   (version :develop true :minor 'inc :patch 'zero :pre-release 'snapshot)
   (watch)
   (test-cljs :cljs-opts test-cljs-options) (speak)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
