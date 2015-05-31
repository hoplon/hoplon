(set-env!
  :source-paths   #{"src"}
  :dependencies  '[[adzerk/boot-reload        "0.2.6"]
                   [pandeiro/boot-http        "0.6.2"]
                   [org.clojure/clojurescript "0.0-3269"]
                   [adzerk/boot-cljs          "0.0-3269-0"]
                   [tailrecursion/boot-hoplon "0.1.0-SNAPSHOT"]
                   ;[tailrecursion/hoplon      "6.0.0-alpha1"]
                   [tailrecursion/castra      "3.0.0-SNAPSHOT"]
                   ])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[tailrecursion.boot-hoplon :refer [haml hoplon prerender html2cljs]])

(deftask dev
  "Build hoplon.io for local development."
  []
  (comp
    #_(haml)
    (hoplon)
    (reload)
    (cljs)
    #_(serve)))
