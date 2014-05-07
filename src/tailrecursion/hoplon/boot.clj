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
    [clojure.string                         :as string]
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
        phantom!  #(let [{:keys [exit out err]} (sh/sh "phantomjs" %1 %2)
                         warn? (and (zero? exit) (not (empty? err)))]
                     (when warn? (println (string/trimr err)))
                     (if (= 0 exit) out (throw (Exception. err))))
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
            (let [rel        (boot/relative-path out)
                  path       (.getPath (io/file tmpdir2 rel))
                  ->frms     #(-> % ts/parse-page ts/pedanticize)
                  forms1     (-> path slurp ->frms)
                  forms2     (-> (phantom! rjs-path path) ->frms)
                  [_ att1 [_ hatt1 & head1] [_ batt1 & body1]] forms1
                  [html* att2 [head* hatt2 & head2] [body* batt2 & body2]] forms2
                  script?    (comp (partial = 'script) first)
                  rm-scripts #(remove script? %)
                  att        (merge att1 att2)
                  hatt       (merge hatt1 hatt2)
                  head       (concat head1 (rm-scripts head2))
                  batt       (merge batt1 batt2)
                  body       (concat (rm-scripts body2) body1)
                  merged     `(~'html (~'head ~hatt ~@head) (~'body ~batt ~@body))]
              (println "•" rel)
              (spit out (ts/print-page "html" merged)))))))))

(boot/deftask hoplon
  "Build Hoplon web application.

  This task accepts an optional map of options to pass to the ClojureScript 
  compiler and/or the Hoplon compiler. The Hoplon compiler recognizes the
  following options:

    :cache         If set to `false` in-memory caching of compiled output is
                   disabled.

    :prerender     If set to `false` PhantomJS pre-rendering of page content
                   is disabled.

    :pretty-print  If set to `true` enables pretty-printed output both in the
                   ClojureScript compiler and in the ClojureScript files
                   created by the Hoplon compiler.

    :source-map    If set to `true` source maps will be created for the build.

  Certain ClojureScript compiler options are overridden by the hoplon task, as
  follows: `:output-to`, `:output-dir`, `:source-map`, `:source-map-path`, and
  `:externs`.

  Other options to ClojureScript (i.e. `:optimizations`, etc.) are passed to
  the ClojureScript compiler as-is."
  [& [cljs-opts]]
  (boot/consume-src! (partial boot/by-ext [".hl"]))
  (let [depfiles       (->> (boot/deps) (map second) (mapcat identity)
                            (filter #(.endsWith (first %) ".hl")))
        hoplon-src     (boot/mksrcdir! ::hoplon-src)
        cljs-out       (boot/mkoutdir! ::cljs-out)
        public-out     (boot/mkoutdir! ::public-out)
        hoplon-opts    (-> cljs-opts (select-keys [:cache :pretty-print]))
        out-path       (or (:output-path cljs-opts) "main.js")
        cljs-opts      (dissoc cljs-opts :output-path :cache)
        compile-file   #(do (println "•" (.getPath %1))
                            (hl/compile-file
                              %1 out-path %2 public-out :opts hoplon-opts))
        compile-string #(do (println "•" %2)
                            (hl/compile-string
                              %1 %2 out-path %3 public-out :opts hoplon-opts))]
    (when (seq depfiles)
      (println "Installing Hoplon dependencies...")
      (doseq [[path dep] depfiles]
        (compile-string (slurp dep) path hoplon-src)))
    (comp
      (boot/with-pre-wrap
        (let [srcs (boot/by-ext [".hl"] (boot/src-files))]
          (println "Compiling Hoplon pages...")
          (doseq [f srcs]
            (compile-file f cljs-out))))
      (task/cljs :output-path out-path :opts cljs-opts)
      (prerender public-out cljs-opts))))

(boot/deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [f]
  (boot/with-pre-wrap
    (assert (.exists (io/file (str f))))
    (-> f str slurp ts/parse-page pp/pprint)))
