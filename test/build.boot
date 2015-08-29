(set-env!
  :resource-paths #{"src"}
  :dependencies   '[[org.clojure/clojurescript "1.7.48"]
                    [adzerk/boot-cljs          "1.7.48-3"]
                    [hoplon/boot-hoplon        "0.1.5"]
                    [hoplon                    "6.0.0-alpha9"]])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[hoplon.boot-hoplon :refer [hoplon prerender]])

(deftask build
  "Build test page."
  []
  (comp (hoplon) (cljs) (prerender)))
