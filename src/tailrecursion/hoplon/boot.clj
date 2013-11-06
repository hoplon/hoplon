;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.boot
  (:require
    [clojure.string                         :as s]
    [tailrecursion.boot.task                :as t]
    [tailrecursion.boot.file                :as f]
    [tailrecursion.boot.deps                :as d]
    [clojure.pprint                         :refer [pprint]]
    [clojure.java.io                        :refer [file make-parents]]
    [clojure.java.shell                     :refer [sh]]
    [tailrecursion.boot.core                :refer [ignored? deftask mk! mkdir! add-sync!]]
    [tailrecursion.hoplon.compiler.compiler :refer [compile-string output-path-for as-forms]]
    [tailrecursion.hoplon.compiler.tagsoup  :refer [parse-page print-page pedanticize]]))

(def renderjs 
  "
var page = require('webpage').create(),
    sys  = require('system'),
    cwd  = sys.env.PWD,
    path = sys.args[1]
    uri  = \"file://\" + cwd + \"/\" + path;

page.open(uri, function(status) {
  setTimeout(function() {
    var html = page.evaluate(function() {
      return document.documentElement.outerHTML;
    });
    console.log(html);
    phantom.exit();
  }, 0);
});")

(defn prerender [boot public-dir cljs-opts]
  (let [{:keys [public src-paths]} @boot
        public    (or public-dir public)
        tmpdir1   (mkdir! boot ::phantom-tmp1)
        tmpdir2   (mkdir! boot ::phantom-tmp2)
        rjs-path  (.getPath (file tmpdir1 "render.js"))
        phantom?  (= 0 (:exit (sh "which" "phantomjs")))]
    (spit rjs-path renderjs) 
    (fn [continue]
      (fn [event]
        (when-not (= false (:prerender cljs-opts))
          (if-not phantom?
            (println "Skipping prerender: phantomjs not found on path")
            (do
              (f/sync :hash tmpdir2 public)
              (let [srcs (->> (file-seq tmpdir2)
                              (map #(.getPath %))
                              (filter #(.endsWith % ".html")))]
                (when (seq srcs) (println "Prerendering Hoplon HTML pages...")) 
                (doseq [path srcs]
                  (let [rel    (subs path (inc (count (.getPath tmpdir2))))
                        out    (file public rel)
                        ->frms #(-> % parse-page pedanticize)
                        empt?  #(= % '(meta {}))
                        forms1 (-> path slurp ->frms)
                        forms2 (-> "phantomjs" (sh rjs-path path) :out ->frms)
                        [_ att1 [_ hatt1 & head1] [_ batt1 & body1]] forms1
                        [html* att2 [head* hatt2 & head2] [body* batt2 & body2]] forms2
                        att    (merge att1 att2)
                        hatt   (merge hatt1 hatt2)
                        head   (list* head* hatt (remove empt? (concat head1 head2))) 
                        batt   (merge batt1 batt2)
                        body   (list* body* batt (concat body1 body2)) 
                        merged (list html* att head body)]
                    (println "•" rel)
                    (spit out (print-page "html" merged)))))))) 
        (continue event)))))

(deftask hoplon
  "Build Hoplon web application."
  [boot & [cljs-opts]]
  (let [{:keys [public src-paths src-static system]} @boot
        depfiles    (->> boot d/deps (map second) (mapcat identity)
                         (filter #(.endsWith (first %) ".hl")))
        hoplon-opts (select-keys cljs-opts [:pretty-print])
        hoplon-tmp  (mkdir! boot ::hoplon-tmp)
        cljs-tmp    (mkdir! boot ::cljs-tmp)
        public-tmp  (mkdir! boot ::public-tmp)
        main-js     (file public-tmp "main.js")
        hl-file?    #(.endsWith (.getPath %) ".hl")
        compile     #(do (println "•" %2)
                         (compile-string %1 %2 main-js %3 public-tmp :opts hoplon-opts))]
    (when (seq depfiles)
      (println "Installing Hoplon dependencies...") (flush)
      (doseq [[path dep] depfiles] (compile (slurp dep) path hoplon-tmp)))
    (add-sync! boot public [public-tmp])
    (swap! boot update-in [:src-paths] into (map #(.getPath %) [cljs-tmp hoplon-tmp]))
    (comp
      (fn [continue]
        (fn [event]
          (mkdir! boot ::cljs-tmp)
          (mkdir! boot ::public-tmp)
          (let [files (->> event :src-files (filter hl-file?))]
            (when (seq files) (println "Compiling Hoplon pages...") (flush)) 
            (doseq [f files] (compile (slurp f) (.getPath f) cljs-tmp))
            (continue event)))) 
      (t/cljs boot :output-to main-js :opts cljs-opts)
      (prerender boot public-tmp cljs-opts))))

(deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [boot f]
  (assert (.exists (file (str f))))
  (-> f str slurp pprint))
