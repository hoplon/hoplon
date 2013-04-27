(ns hlisp.core
  (:use
    [hlisp.colors             :only [style pr-ok]]
    [criterium.core           :only [time-body]]
    [clojure.java.io          :only [copy file make-parents reader resource]]
    [clojure.stacktrace       :only [print-stack-trace]]
    [clojure.pprint           :only [pprint]])
  (:require
    [hlisp.sync               :as sync]
    [clojure.java.shell       :as shell]
    [hlisp.compiler           :as hlc]
    [hlisp.tagsoup            :as ts]
    [clojure.string           :as string]
    [cljs.closure             :as closure]))

(def CWD (System/getProperty "user.dir"))

(defn munge-path [path]
  (->
    (str "__" path)
    (string/replace "_" "__")
    (string/replace "/" "_")))

(defn tmpfile [prefix postfix]
  (let [t (java.io.File/createTempFile prefix postfix)]
    (.deleteOnExit t) 
    t))

(defn srcdir->outdir [fname srcdir outdir]
  (.getPath (file outdir (.getPath (.relativize (.toURI (file srcdir))
                                                (.toURI (file fname)))))))

(defn delete-all
  [dir]
  (if (and dir (.exists (file dir))) 
    (mapv #(.delete %) (filter #(.isFile %) (file-seq (file dir))))))

(defn copy-with-lastmod
  [src-file dst-file]
  (copy src-file dst-file)
  (.setLastModified dst-file (.lastModified src-file)))

(defn copy-files
  [src dest]
  (if (.exists (file src))
    (let [files  (map #(.getPath %) (filter #(.isFile %) (file-seq (file src)))) 
          outs   (map #(srcdir->outdir % src dest) files)
          srcs   (map file files)
          dsts   (map file outs)]
      (mapv make-parents dsts)
      (mapv copy-with-lastmod srcs dsts))))

(defn compile-file
  [f js-uri html-work cljs-work stage-work base-uri]
  (let [path      (.getPath f)
        to-html   (if (.endsWith path ".cljs")
                    (string/replace path #"\.cljs$" ".html")
                    path)
        html-file (file (srcdir->outdir to-html html-work stage-work))
        cljs-file (file (str cljs-work "/" (munge-path path) ".cljs"))] 
    (when-let [compiled (hlc/compile-file f js-uri base-uri)] 
      (mapv make-parents [html-file cljs-file])
      (spit html-file (:html compiled)) 
      (spit cljs-file (:cljs compiled)))))

(def is-file?     #(.isFile %))
(def last-html    (atom {}))
(def last-cljs    (atom {}))
(def last-include (atom {}))

(defn hlisp-compile
  [{:keys [html-out   outdir-out  base-dir      includes    cljsc-opts
           html-work  cljs-work   include-work  out-work    stage-work
           cljs-dep   inc-dep     ext-dep       lib-dep     flib-dep
           html-src   cljs-src    include-src   static-src  cljs-stage]}]

  (delete-all html-work)
  (delete-all cljs-work)
  (delete-all include-work)

  (copy-files html-src html-work)
  (copy-files cljs-src cljs-work)
  (copy-files include-src include-work)
  (copy-files cljs-dep cljs-work)

  (let [page-files  (filter is-file? (file-seq (file html-work))) 
        incs        (mapv #(.getPath %) (filter is-file? (file-seq (file inc-dep)))) 
        exts        (mapv #(.getPath %) (filter is-file? (file-seq (file ext-dep)))) 
        libs        (mapv #(.getPath %) (filter is-file? (file-seq (file lib-dep))))
        env-str     (slurp (reader (resource "env.cljs")))
        env-tmp     (file cljs-work "____env.cljs")
        js-tmp      (tmpfile "____hlisp_" ".js")
        js-tmp-path (.getPath js-tmp)
        js-uri      (.getPath
                      (.relativize (.toURI (file CWD))
                                   (.toURI (file (file base-dir) "main.js"))))
        js-out      (file stage-work "main.js")
        base-uri    (and (nil? (:optimizations cljsc-opts))
                         (.getPath
                           (.relativize
                             (.toURI (file CWD))
                             (.toURI (file (file base-dir) "out" "goog" "base.js"))))) 
        options     (->
                      (assoc cljsc-opts
                             :output-to     js-tmp-path
                             :output-dir    out-work)
                      (update-in [:externs] into exts)
                      (update-in [:libs] into libs))
        all-incs    (into (vec (reverse (sort incs))) includes)]
    (spit env-tmp env-str)
    (mapv #(compile-file % js-uri html-work cljs-work stage-work base-uri) page-files)
    (sync/sync-hash cljs-stage cljs-work)
    (closure/build cljs-stage options)
    (when outdir-out (copy-files out-work outdir-out)) 
    (spit js-out (string/join "\n" (map slurp (conj all-incs js-tmp-path))))
    (.delete js-tmp)))

(defn elapsed-sec
  "Given a function f and arguments, applies f to the arguments and returns the
  elapsed time in seconds."
  [f & args]
  (float (/ (first (time-body (apply f args))) 1000000000)))

(defn print* [& args]
  (apply print args)
  (flush))

(defn println* [& args]
  (apply println args)
  (flush))

(defn run-script [script]
  (when (.exists (file script))
    (print* (style (str (java.util.Date.) " << ") :blue))
    (print* (style (str "script " script) :bold-blue))
    (println* (style " >> " :blue))
    (let [res (shell/sh script)
          out (:out res)
          err (:err res)]
      (print* out)
      (print* (style err :red)))))

(defn compile-fancy [{:keys [pre-script post-script] :as opts}]
  (try
    (run-script pre-script)
    (print* (style (str (java.util.Date.) " << ") :blue))
    (print* (style "compiling" :bold-blue))
    (println* (style " >> " :blue))
    (let [t (elapsed-sec hlisp-compile opts)]
      (print* (style (str (java.util.Date.) " << ") :blue))
      (print* (-> (format "%.3f sec." t) (style :green)))
      (println* (style " >> " :blue))) 
    (run-script post-script)
    (catch Throwable e
      (println* (style "Dang!" :red))
      (print* (style (with-out-str (print-stack-trace e)) :red)))))

(defn prepare [{:keys [work-dir html-out outdir-out html-work cljs-work
                       include-work stage-work] :as opts}]
  (delete-all work-dir)
  (mapv #(make-parents (file % "foo"))
        [html-out html-work cljs-work include-work stage-work outdir-out])
  (delete-all html-out)
  (delete-all outdir-out))

(defn start [{:keys [html-work  cljs-work   include-work  stage-work
                     static-src html-src    cljs-src      include-src
                     work-dir   outdir-out  html-out] :as opts} & {:keys [auto]}]
  (let [dirs    [html-src cljs-src include-src]
        exts    #{".cljs" ".html"}
        mods    #(apply sync/dir-map-ext exts %)
        changes #(reduce into #{} (sync/what-changed %1 %2))]
    (loop [before {} now (mods dirs)]
      (when (seq (changes before now)) (compile-fancy opts))
      (sync/sync html-out stage-work static-src)
      (Thread/sleep 100)
      (when auto (recur now (mods dirs))))))
