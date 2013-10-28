;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.boot
  (:require
    [tailrecursion.boot.task                :as t]
    [tailrecursion.boot.file                :as f]
    [tailrecursion.hoplon.compiler.pprint   :as p]
    [clojure.pprint                         :refer [pprint]]
    [clojure.java.io                        :refer [file make-parents]]
    [clojure.java.shell                     :refer [sh]]
    [tailrecursion.boot.core                :refer [ignored? deftask mk! mkdir! add-sync!]]
    [tailrecursion.hoplon.compiler.compiler :refer [compile-file output-path-for as-forms-string as-forms-path]]
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

(deftask prerender
  "Prepopulates Hoplon page with content scraped from phantomjs."
  [boot]
  (let [{:keys [public src-paths]} @boot
        tmpdir    (mkdir! boot ::phantom-tmp)
        rjs-path  (.getPath (file tmpdir "render.js"))]
    (when-not (= 0 (:exit (sh "which" "phantomjs")))
      (throw (Exception. "No phantomjs on path"))) 
    (spit rjs-path renderjs) 
    (fn [continue]
      (fn [event]
        (let [srcs (-> (->> (:src-files event) (map #(.getPath %)))
                       (->> (filter #(.endsWith % ".hl")) (map output-path-for)))]
          (doseq [path (map #(str public "/" %) srcs)]
            (let [->frms #(-> % (as-forms-string "html") first pedanticize)
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
              (spit path (pp-forms "html" merged)))))))))

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
           (doseq [f files] (compile f))
           (% event)))
      (t/cljs boot :output-to main-js :opts cljs-opts))))

(deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [boot f]
  (assert (.exists (file (str f))))
  (-> f str as-forms-path pprint))
