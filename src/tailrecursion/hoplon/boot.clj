;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.boot
  {:boot/export-tasks true}
  (:require [boot.core                              :as boot]
            [clojure.pprint                         :as pp]
            [clojure.java.io                        :as io]
            [clojure.string                         :as string]
            [clojure.java.shell                     :as sh]
            [tailrecursion.hoplon.compiler.compiler :as hl]
            [tailrecursion.hoplon.compiler.tagsoup  :as ts]))

(def renderjs
  "
var page = require('webpage').create(),
    sys  = require('system'),
    path = sys.args[1]
    uri  = \"file://\" + path;
page.open(uri, function(status) {
  setTimeout(function() {
    var html = page.evaluate(function() {
      return document.documentElement.outerHTML;
    });
    console.log(html);
    phantom.exit();
  }, 0);
});")

(boot/deftask prerender
  [e engine ENGINE str "PhantomJS-compatible engine to use."]
  (let [engine       (or engine "phantomjs")
        tmp          (boot/temp-dir!)
        prev-fileset (atom nil)
        rjs-tmp      (boot/temp-dir!)
        rjs-path     (.getPath (io/file rjs-tmp "render.js"))
        win?         (#{"Windows_NT"} (System/getenv "OS"))
        phantom?     (= 0 (:exit (sh/sh (if win? "where" "which") engine)))
        phantom!     #(let [{:keys [exit out err]} (sh/sh engine %1 %2)
                            warn? (and (zero? exit) (not (empty? err)))]
                       (when warn? (println (string/trimr err)))
                       (if (= 0 exit) out (throw (Exception. err))))
        not-found    #(println "Skipping prerender: " engine " not found on path.")]
    (spit rjs-path renderjs)
    (if (not phantom?)
      (do (not-found) identity)
      (boot/with-pre-wrap fileset
        (println "Prerendering HTML pages...")
        (let [html (->> fileset
                        (boot/fileset-diff @prev-fileset)
                        boot/output-files
                        (boot/by-ext [".html"]))]
          (reset! prev-fileset fileset)
          (doseq [f html]
            (let [path (-> f boot/tmpfile .getPath)
                  ->frms #(-> % ts/parse-page ts/pedanticize)
                  forms1 (-> path slurp ->frms)
                  forms2 (-> (phantom! rjs-path path) ->frms)
                  [_ att1 [_ hatt1 & head1] [_ batt1 & body1]] forms1
                  [html* att2 [head* hatt2 & head2] [body* batt2 & body2]] forms2
                  script? (comp (partial = 'script) first)
                  rm-scripts #(remove script? %)
                  att (merge att1 att2)
                  hatt (merge hatt1 hatt2)
                  head (concat head1 (rm-scripts head2))
                  batt (merge batt1 batt2)
                  body (concat (rm-scripts body2) body1)
                  merged `(~'html ~att (~'head ~hatt ~@head) (~'body ~batt ~@body))]
              (println "•" path)
              (spit (io/file tmp (boot/tmppath f))
                    (ts/print-page "html" merged))))
          (-> fileset
              (boot/add-resource tmp)
              boot/commit!))))))

(boot/deftask hoplon
  "Build Hoplon web application.

  This task accepts an optional map of options to pass to the Hoplon compiler.
  Further ClojureScript compilation rely on another task (e. g. boot-cljs).
  The Hoplon compiler recognizes the following options:

  * :pretty-print  If set to `true` enables pretty-printed output
                   in the ClojureScript files created by the Hoplon compiler.

  If you are compiling library, you need to include resulting cljs in target.
  Do it by specifying :lib flag."
  [pp pretty-print bool "Pretty-print CLJS files created by the Hoplon compiler."
   l  lib          bool "Include produced cljs in the final artefact."]
  (let [tmp-cljs (boot/temp-dir!)
        tmp-html (boot/temp-dir!)
        prev-fileset (atom nil)
        opts (dissoc *opts* :lib)
        add-cljs (if lib boot/add-resource boot/add-source)]
    (boot/with-pre-wrap fileset
      (println "Compiling Hoplon pages...")
      (boot/empty-dir! tmp-html)
      (let [hl (->> fileset
                    (boot/fileset-diff @prev-fileset)
                    boot/input-files
                    (boot/by-ext [".hl"])
                    (map boot/tmpfile))]
        (reset! prev-fileset fileset)
        (doseq [f hl]
          (println "•" (.getPath f))
          (hl/compile-file f tmp-cljs tmp-html :opts opts)))
      (-> fileset
          (add-cljs tmp-cljs)
          (boot/add-resource tmp-html)
          boot/commit!))))

(boot/deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [f file FILENAME str "File to convert."]
  (boot/with-pre-wrap fileset
    (->> file str slurp hl/as-forms
         (#(with-out-str (pp/write % :dispatch pp/code-dispatch)))
         clojure.string/trim
         (#(subs % 1 (dec (count %))))
         print)
    fileset))