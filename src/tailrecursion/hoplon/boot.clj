;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.boot
  (:require
    [clojure.pprint                         :as pp]
    [clojure.java.io                        :as io]
    [clojure.java.shell                     :as sh]
    [tailrecursion.boot.core                :as boot]
    [tailrecursion.boot.task                :as task]
    [tailrecursion.boot.file                :as file]
    [tailrecursion.hoplon.compiler.compiler :as hl]
    [tailrecursion.hoplon.compiler.tagsoup  :as ts]))

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

(defn prerender [public cljs-opts]
  (let [tmpdir1   (boot/mktmpdir! ::phantom-tmp1)
        tmpdir2   (boot/mktmpdir! ::phantom-tmp2)
        skip?     (= false (:prerender cljs-opts))
        rjs-path  (.getPath (io/file tmpdir1 "render.js"))
        win?      (#{"Windows_NT"} (System/getenv "OS"))
        phantom?  (= 0 (:exit (sh/sh (if win? "where" "which") "phantomjs")))
        not-found #(println "Skipping prerender: phantomjs not found on path.")]
    (spit rjs-path renderjs)
    (cond
      skip?          identity
      (not phantom?) (do (not-found) identity)
      :else
      (boot/with-pre-wrap
        (apply file/sync :hash tmpdir2 @boot/outdirs)
        (let [srcs (->> (boot/out-files) (boot/by-ext [".html"]))]
          (when (seq srcs) (println "Prerendering Hoplon HTML pages..."))
          (doseq [out srcs]
            (let [rel    (boot/relative-path out)
                  path   (.getPath (io/file tmpdir2 rel))
                  ->frms #(-> % ts/parse-page ts/pedanticize)
                  forms1 (-> path slurp ->frms)
                  forms2 (-> "phantomjs" (sh/sh rjs-path path) :out ->frms)
                  [_ att1 [_ hatt1 & head1] [_ batt1 & body1]] forms1
                  [html* att2 [head* hatt2 & head2] [body* batt2 & body2]] forms2
                  att    (merge att1 att2)
                  hatt   (merge hatt1 hatt2)
                  head   (list* 'head hatt1 head1)
                  batt   (merge batt1 batt2)
                  body   (list* body* batt (concat body2 body1))
                  merged (list html* att head body)]
              (println "•" rel)
              (spit out (ts/print-page "html" merged)))))))))

(boot/deftask hoplon
  "Build Hoplon web application."
  [& [cljs-opts]]
  (boot/consume-src! (partial boot/by-ext [".hl"]))
  (let [depfiles     (->> (boot/deps) (map second) (mapcat identity)
                          (filter #(.endsWith (first %) ".hl")))
        hoplon-opts  (select-keys cljs-opts [:pretty-print])
        hoplon-src   (boot/mksrcdir! ::hoplon-src)
        cljs-tmp     (boot/mktmpdir! ::cljs-tmp)
        public-tmp   (boot/mktmpdir! ::public-tmp)
        cljs-stage   (boot/mkoutdir! ::cljs-stage)
        public-stage (boot/mkoutdir! ::public-stage)
        main-js      (io/file public-tmp "main.js")
        compile      #(do (println "•" %2)
                          (hl/compile-string %1 %2 main-js %3 public-tmp :opts hoplon-opts))]
    (when (seq depfiles)
      (println "Installing Hoplon dependencies...")
      (doseq [[path dep] depfiles] (compile (slurp dep) path hoplon-src)))
    (if-let [static (seq (boot/get-env :src-static))]
      (boot/add-sync! (boot/get-env :out-path) static))
    (comp
      (boot/with-pre-wrap
        (when-let [srcs (-> (boot/by-ext [".hl"] (boot/src-files))
                          (boot/newer? cljs-stage public-stage))]
          (println "Compiling Hoplon pages...")
          (doseq [f srcs] (compile (slurp f) (.getPath f) cljs-tmp)))
        (boot/sync! cljs-stage cljs-tmp)
        (boot/sync! public-stage public-tmp))
      (task/cljs :output-to "main.js" :opts cljs-opts)
      (prerender public-stage cljs-opts))))

(boot/deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [f]
  (boot/with-pre-wrap
    (assert (.exists (io/file (str f))))
    (-> f str slurp ts/parse-page pp/pprint)))
