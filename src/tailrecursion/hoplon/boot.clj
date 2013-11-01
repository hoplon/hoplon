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
    [tailrecursion.hoplon.compiler.pprint   :as p]
    [clojure.pprint                         :refer [pprint]]
    [clojure.java.io                        :refer [file make-parents]]
    [clojure.java.shell                     :refer [sh]]
    [tailrecursion.boot.core                :refer [ignored? deftask mk! mkdir! add-sync!]]
    [tailrecursion.hoplon.compiler.compiler :refer [compile-string output-path-for as-forms]]
    [tailrecursion.hoplon.compiler.tagsoup  :refer [pp-forms pedanticize]]))

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
        tmpdir    (mkdir! boot ::phantom-tmp)
        rjs-path  (.getPath (file tmpdir "render.js"))
        phantom?  (= 0 (:exit (sh "which" "phantomjs")))]
    (spit rjs-path renderjs) 
    (fn [continue]
      (fn [event]
        (when-not (= false (:prerender cljs-opts))
          (if-not phantom?
            (println "Skipping prerender: phantomjs not found on path")
            (let [page? #(and (.exists %)
                              (.endsWith (.getPath %) ".hl")
                              (= 'page (-> % slurp as-forms ffirst)))
                  srcs  (->> (get-in event [:watch :time]) (filter page?)
                             (map #(.getPath %)) (map output-path-for))]
              (when (seq srcs) (println "Prerendering Hoplon HTML pages...")) 
              (doseq [path (map #(str public "/" %) srcs)]
                (let [->frms #(-> % as-forms first pedanticize)
                      forms1 (-> path slurp ->frms)
                      forms2 (-> "phantomjs" (sh rjs-path path) :out ->frms)
                      [_ att1 [_ hatt1 & head1] [_ batt1 & body1]] forms1
                      [html* att2 [head* hatt2 & head2] [body* batt2 & body2]] forms2
                      empt?  #(= % '(meta {}))
                      att    (merge att1 att2)
                      hatt   (merge hatt1 hatt2)
                      head   (list* head* hatt (remove empt? (concat head1 head2))) 
                      batt   (merge batt1 batt2)
                      body   (list* body* batt (concat body1 body2)) 
                      merged (list html* att head body)]
                  (spit path (pp-forms "html" merged))))))) 
        (continue event)))))

(deftask hoplon
  "Build Hoplon web application."
  [boot & [cljs-opts]]
  (let [{:keys [public src-paths src-static system]} @boot
        depfiles    (->> boot d/deps (map second) (mapcat identity)
                         (filter #(.endsWith (first %) ".hl")))
        hoplon-opts (select-keys cljs-opts [:pretty-print])
        cljs-tmp    (mkdir! boot ::cljs-tmp)
        public-tmp  (mkdir! boot ::public-tmp)
        main-js     (file public-tmp "main.js")
        compile     #(compile-string %1 %2 main-js cljs-tmp public-tmp :opts hoplon-opts)]
    (when (seq depfiles)
      (println "Installing Hoplon dependencies...") (flush)
      (doseq [[path dep] depfiles] (compile (slurp dep) path)))
    (add-sync! boot public [public-tmp])
    (swap! boot update-in [:src-paths] conj (.getPath cljs-tmp))
    (comp
      #(fn [event]
         (let [files (get-in event [:watch :time])]
           (when (seq files) (println "Compiling Hoplon pages...") (flush)) 
           (doseq [f files] (compile (slurp f) (.getPath f)))
           (% event)))
      (t/cljs boot :output-to main-js :opts cljs-opts)
      (prerender boot public-tmp cljs-opts))))

(deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [boot f]
  (assert (.exists (file (str f))))
  (-> f str slurp pprint))
