(ns tailrecursion.hoplon.boot
  (:require
    [tailrecursion.boot.task                  :as t]
    [tailrecursion.boot.file                  :as f]
    [clojure.pprint                           :refer [pprint]]
    [clojure.java.io                          :refer [file make-parents]]
    [tailrecursion.boot.core                  :refer [ignored? deftask mk! mkdir! add-sync!]]
    [tailrecursion.hoplon.compiler.compiler   :refer [compile-file]]
    [tailrecursion.hoplon.compiler.tagsoup    :refer [parse-string tagsoup->hoplon]]))

(deftask hoplon
  "Build Hoplon web application."
  [boot & [cljs-opts]]
  (let [{:keys [public src-paths src-static system]} @boot
        hoplon-opts (select-keys cljs-opts [:pretty-print])
        cljs-tmp    (mkdir! boot ::cljs-tmp)
        public-tmp  (mkdir! boot ::public-tmp)
        main-js     (file public-tmp "main.js")
        compile     #(compile-file % main-js cljs-tmp public-tmp :opts hoplon-opts)]
    (add-sync! boot public (into [public-tmp] (map file src-static)))
    (swap! boot update-in [:src-paths] conj (.getPath cljs-tmp))
    (comp
      #(fn [event]
         (let [files (or (get-in event [:watch :time]) (:src-files event))]
           (mkdir! boot ::cljs-tmp) 
           (mkdir! boot ::public-tmp) 
           (doall (map compile files))
           (% event)))
      (t/cljs boot :output-to main-js :opts cljs-opts))))

(deftask html2cljs
  "Print cljs representation of an HTML file."
  [boot f]
  (assert (.exists (file (str f))))
  (-> f str slurp parse-string tagsoup->hoplon pprint))
