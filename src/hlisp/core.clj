(ns hlisp.core
  (:use
    [hlisp.colors             :only [style pr-ok print println]]
    [criterium.core           :only [time-body]]
    [clojure.java.io          :only [copy file make-parents reader resource]]
    [clojure.stacktrace       :only [print-stack-trace]])
  (:require
    [clojure.java.shell       :as shell]
    [hlisp.util.file          :as f]
    [hlisp.compiler           :as hlc]
    [hlisp.tagsoup            :as ts]
    [clojure.string           :as string]
    [cljs.closure             :as closure])
  (:refer-clojure :exclude [print println]))

(def CWD      (System/getProperty "user.dir"))
(def is-file? #(.isFile %))

(defn elapsed-sec
  [f & args]
  (float (/ (first (time-body (apply f args))) 1000000000)))

(defn run-script [script]
  (when (.exists (file script))
    (print (style (str (java.util.Date.) " << ") :blue))
    (print (style (str "script " script) :bold-blue))
    (println (style " >> " :blue))
    (let [res (shell/sh script)
          out (:out res)
          err (:err res)]
      (print out)
      (print (style err :red)))))

(defn munge-path
  [path]
  (-> (str "__" path)
    (string/replace "_" "__")
    (string/replace "/" "_")))

(defn compile-file
  [f js-uri html-work cljs-work stage-work base-uri]
  (let [path      (.getPath f)
        to-html   (if (.endsWith path ".cljs")
                    (string/replace path #"\.cljs$" ".html")
                    path)
        html-file (file (f/srcdir->outdir to-html html-work stage-work))
        cljs-file (file (str cljs-work "/" (munge-path path) ".cljs"))] 
    (when-let [compiled (hlc/compile-file f js-uri base-uri)] 
      (mapv make-parents [html-file cljs-file])
      (spit html-file (:html compiled)) 
      (spit cljs-file (:cljs compiled)))))

(defn hlisp-compile
  [{:keys [html-out   outdir-out  base-dir      includes    cljsc-opts
           html-work  cljs-work   include-work  out-work    stage-work
           cljs-dep   inc-dep     ext-dep       lib-dep     flib-dep
           html-src   cljs-src    include-src   static-src  cljs-stage]}]

  (f/delete-all html-work)
  (f/delete-all cljs-work)
  (f/delete-all include-work)

  (f/copy-files html-src html-work)
  (f/copy-files cljs-src cljs-work)
  (f/copy-files include-src include-work)
  (f/copy-files cljs-dep cljs-work)

  (let [page-files  (filter is-file? (file-seq (file html-work))) 
        incs        (mapv #(.getPath %) (filter is-file? (file-seq (file inc-dep)))) 
        exts        (mapv #(.getPath %) (filter is-file? (file-seq (file ext-dep)))) 
        libs        (mapv #(.getPath %) (filter is-file? (file-seq (file lib-dep))))
        env-str     (slurp (reader (resource "env.cljs")))
        env-tmp     (file cljs-work "____env.cljs")
        js-tmp      (f/tmpfile "____hlisp_" ".js")
        js-tmp-path (.getPath js-tmp)
        js-uris     (mapv #(f/up-parents % html-work "main.js") page-files) 
        js-out      (file stage-work "main.js")
        optimize?   (:optimizations cljsc-opts)
        base-uris   (mapv
                      #(when-not optimize?
                         (f/up-parents % html-work outdir-out "goog" "base.js"))
                      page-files)
        options     (->
                      (assoc cljsc-opts
                             :output-to     js-tmp-path
                             :output-dir    out-work)
                      (update-in [:externs] into exts)
                      (update-in [:libs] into libs))
        all-incs    (into (vec (reverse (sort incs))) includes)]
    (spit env-tmp env-str)
    (mapv #(compile-file %1 %2 html-work cljs-work stage-work %3) page-files js-uris base-uris)
    (f/sync-hash cljs-stage cljs-work)
    (closure/build cljs-stage options)
    (when-not optimize? (f/copy-files out-work (file stage-work outdir-out))) 
    (spit js-out (string/join "\n" (map slurp (conj all-incs js-tmp-path))))
    (.delete js-tmp)))

(defn compile-fancy
  [{:keys [pre-script post-script] :as opts}]
  (try
    (run-script pre-script)
    (print (style (str (java.util.Date.) " << ") :blue))
    (print (style "compiling" :bold-blue))
    (println (style " >> " :blue))
    (let [t (elapsed-sec hlisp-compile opts)]
      (print (style (str (java.util.Date.) " << ") :blue))
      (print (-> (format "%.3f sec." t) (style :green)))
      (println (style " >> " :blue))) 
    (run-script post-script)
    (catch Throwable e
      (println (style "Dang!" :red))
      (print (style (with-out-str (print-stack-trace e)) :red)))))

(defn prepare
  [{:keys [work-dir html-out outdir-out html-work cljs-work
           include-work stage-work] :as opts}]
  (f/delete-all work-dir)
  (mapv #(make-parents (file % "foo"))
        [html-out html-work cljs-work include-work stage-work outdir-out])
  (f/delete-all html-out)
  (f/delete-all outdir-out))

(defn start
  [{:keys [html-work  cljs-work   include-work  stage-work
           static-src html-src    cljs-src      include-src
           work-dir   outdir-out  html-out] :as opts} & {:keys [auto]}]
  (let [dirs    [html-src cljs-src include-src]
        exts    #{".cljs" ".html"}
        mods    #(apply f/dir-map-ext exts %)
        changes #(reduce into #{} (f/what-changed %1 %2))]
    (loop [before {} now (mods dirs)]
      (when (seq (changes before now)) (compile-fancy opts))
      (f/sync html-out stage-work static-src)
      (Thread/sleep 100)
      (when auto (recur now (mods dirs))))))
