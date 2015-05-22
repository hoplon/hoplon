(set-env!
  ;; using the sonatype repo is sometimes useful when testing Clojurescript
  ;; versions that not yet propagated to Clojars
  ;; :repositories #(conj % '["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"}])
  :dependencies '[[org.clojure/clojure   "1.6.0"      :scope "provided"]
                  [boot/core             "2.0.0-rc10" :scope "provided"]
                  [adzerk/bootlaces      "0.1.10"     :scope "test"]
                  [cljsjs/jquery         "1.8.2-2"    :scope "compile"]
                  [tailrecursion/javelin "3.7.2"      :scope "compile"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "6.0.0-alpha1")

(bootlaces! +version+)

(task-options!
  pom  {:project     'tailrecursion/hoplon
        :version     +version+
        :description "Hoplon web development environment."
        :url         "https://github.com/tailrecursion/hoplon"
        :scm         {:url "https://github.com/tailrecursion/hoplon"}
        :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
