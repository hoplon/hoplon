(ns tailrecursion.hoplon.boot
  (:require
    [tailrecursion.boot.task                  :as t]
    [tailrecursion.boot.file                  :as f]
    [tailrecursion.boot.core                  :refer [deftask mk! mkdir! add-sync!]]
    [clojure.java.io                          :refer [file make-parents]]
    [tailrecursion.hoplon.compiler.compiler   :refer [compile-dirs]]))

(deftask hoplon
  "Build Hoplon web application."
  [boot & [cljs-opts]]
  (let [{:keys [public src-paths src-static system]} @boot
        hoplon-opts (select-keys cljs-opts [:pretty-print])
        src-paths   (map file src-paths)
        cljs-tmp    (mkdir! boot ::cljs-tmp)
        public-tmp  (mkdir! boot ::public-tmp)
        main-js     (file public-tmp "main.js")]
    (add-sync! boot public (into [public-tmp] (map file src-static)))
    (swap! boot update-in [:src-paths] conj (.getPath cljs-tmp))
    (comp
      #(fn [event]
         (mkdir! boot ::cljs-tmp)
         (mkdir! boot ::public-tmp)
         (compile-dirs main-js src-paths cljs-tmp public-tmp :opts hoplon-opts)
         (% event))
      (t/cljs boot :output-to main-js :opts cljs-opts))))
