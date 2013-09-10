(ns tailrecursion.hoplon.boot
  (:require
    [tailrecursion.boot.task                  :as t]
    [tailrecursion.boot.file                  :as f]
    [tailrecursion.boot.core                  :refer [deftask]]
    [clojure.java.io                          :refer [file make-parents]]
    [tailrecursion.boot.tmpregistry           :refer [mk! mkdir! add-sync!]]
    [tailrecursion.hoplon.compiler.compiler   :refer [compile-dirs]]))

(deftask hoplon
  "Build Hoplon web application."
  [boot & [cljs-opts]]
  (let [{:keys [public src-paths system static]} @boot
        tmp         (:tmpregistry system)
        static-dir  (file static)
        src-paths   (map file src-paths)
        cljs-tmp    (mkdir! tmp ::cljs-tmp)
        public-tmp  (mkdir! tmp ::public-tmp)
        main-js     (file public-tmp "main.js")]
    (add-sync! tmp public [public-tmp static-dir])
    (swap! boot update-in [:src-paths] conj (.getPath cljs-tmp))
    (comp
      #(fn [event]
         (mkdir! tmp ::cljs-tmp)
         (mkdir! tmp ::public-tmp)
         (compile-dirs main-js src-paths cljs-tmp public-tmp)
         (% event))
      (t/cljs boot :output-to main-js :opts cljs-opts))))
