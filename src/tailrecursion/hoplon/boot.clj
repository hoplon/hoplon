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

(defn trim    [s]       (try (string/trim s) (catch Throwable _)))
(defn do-once [state f] #(when (compare-and-set! state nil true) (f)))
(defn trans   [coll]    (when-not (empty? coll) (apply map vector coll)))

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
                  merged     `(~'html ~att (~'head ~hatt ~@head) (~'body ~batt ~@body))]
              (println "•" rel)
              (spit out (ts/print-page "html" merged)))))))))

(defn srcfiles []
  (->> (boot/get-env :src-paths)
    (mapcat (comp file-seq io/file))
    (filter #(.isFile %))
    (sort-by #(.getName %))
    (map (juxt boot/relative-path identity))))

(defn depfiles []
  (->> (boot/deps)
    (map second)
    (mapcat (partial sort-by #(.getName (io/file (first %)))))))

(defn copy-resource
  [resource-path out-path]
  (when-not (.endsWith resource-path "/")
    (with-open [in  (io/input-stream (io/resource resource-path))
                out (io/output-stream (io/file out-path))]
      (io/copy in out))))

(defn get-imports
  "Separates out the @imports from the stylesheet, preserving leading comments.
  Returns a vector with two strings: `[imports-str rules-str]`."
  [css-str]
  (let [re-import  #"(?is)^\s*(@import[^;]+;)(.*)"
        re-comment #"(?is)^\s*(/\*[^*]*\*+([^/*][^*]*\*+)*/)(.*)"]
    (loop [imports [] comments [] css css-str]
      (let [[_ m1 s1]   (map trim (re-find re-import css))
            [_ m2 _ s2] (map trim (re-find re-comment css))]
        (cond
          s1    (recur (conj imports m1) comments s1)
          s2    (recur imports (conj comments m2) s2)
          :else [(string/join "\n" (remove string/blank? imports))
                 (string/join "\n" (remove string/blank? (conj comments css)))])))))

(defn get-css [files]
  (->> files
    (filter #(re-find #"\.inc\.css$" (first %)))
    (map (comp get-imports slurp second))
    trans
    (map (partial string/join "\n"))))

(defn install-res [state dep-res-dir src-res-dir]
  (let [outpath #(subs % (count "_hoplon/"))
        outfile #(doto (io/file %1 %2) io/make-parents)
        filter* (partial filter #(re-find #"^_hoplon/.+$" (first %)))
        copysrc #(io/copy (second %) (outfile src-res-dir (outpath (first %))))
        copyres (fn [[f & _]] (copy-resource f (outfile dep-res-dir (outpath f))))
        write   #(do (doall (map %2 (filter* %1))) ::ok)
        do-deps (do-once state #(write (depfiles) copyres))]
    (do-deps)
    (write (srcfiles) copysrc)))

(def      filename-uuid         "c6f4dce0-0384-11e4-9191-0800200c9a66")
(defmacro filename-uuid-cljs [] filename-uuid)

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

    :output-path   Specify the path (relative to the docroot) where the compiled
                   JavaScript file should be created. Default is \"main.js\".

    :css-inc-path  Specify the path (relative to the docroot) where the included
                   CSS files (i.e. the stylesheets on the classpath with the
                   `.inc.css` extension) should be concatenated and written to.
                   Default is \"main.css\".

  Certain ClojureScript compiler options are overridden by the hoplon task, as
  follows: `:output-to`, `:output-dir`, `:source-map`, `:source-map-path`, and
  `:externs`.

  Other options to ClojureScript (i.e. `:optimizations`, etc.) are passed to
  the ClojureScript compiler as-is."
  [& [cljs-opts]]
  (boot/consume-src! (partial boot/by-ext [".hl"]))
  (let [[css-imports css-rules] (get-css (depfiles))
        hoplon-src     (boot/mksrcdir! ::hoplon-src)
        cljs-out       (boot/mkoutdir! ::cljs-out)
        public-out     (boot/mkoutdir! ::public-out)
        src-inc-css    (boot/mkoutdir! ::hoplon-src-inc-css)
        dep-inc-res    (boot/mktmpdir! ::hoplon-dep-inc-res)
        src-inc-res    (boot/mktmpdir! ::hoplon-src-inc-res)
        install-res?   (atom nil)
        hoplon-opts    (-> cljs-opts
                         (select-keys [:cache :pretty-print :css-inc-path])
                         (update-in [:css-inc-path] #(or % (str filename-uuid ".css"))))
        css-inc-file   (io/file src-inc-css (:css-inc-path hoplon-opts))
        out-path       (or (:output-path cljs-opts) (str filename-uuid ".js"))
        cljs-opts      (dissoc cljs-opts :output-path :css-inc-path :cache)
        compile-file   #(do (println "•" (.getPath %1))
                            (hl/compile-file
                              %1 out-path %2 public-out :opts hoplon-opts))
        compile-string #(do (println "•" %2)
                            (hl/compile-string
                              %1 %2 out-path %3 public-out :opts hoplon-opts))]
    (boot/add-sync! (boot/get-env :out-path) #{dep-inc-res src-inc-res})
    (println "Compiling Hoplon dependencies...")
    (doseq [[path dep] (filter #(re-find #"\.hl$" (first %)) (depfiles))]
      (compile-string (slurp dep) path hoplon-src))
    (comp
      (boot/with-pre-wrap
        (let [srcs        (boot/by-ext [".hl"] (boot/src-files))
              src-inc-res (boot/mktmpdir! ::hoplon-src-inc-res)]
          (println "Compiling Hoplon pages...")
          (let [[imports rules] (get-css (srcfiles))]
            (->> [css-imports imports css-rules rules]
              (remove string/blank?)
              (string/join "\n")
              (spit css-inc-file)))
          (install-res install-res? dep-inc-res src-inc-res)
          (doseq [f srcs]
            (compile-file f cljs-out))))
      (task/cljs :output-path out-path :opts cljs-opts)
      (prerender public-out cljs-opts))))

(boot/deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [f]
  (boot/with-pre-wrap
    (assert (.exists (io/file (str f))))
    (->> f str slurp ts/read-hiccup ts/parse-hiccup
      (#(with-out-str (pp/write % :dispatch pp/code-dispatch))) print)))
