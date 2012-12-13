(ns hlisp.core
  (:use
    [hlisp.watchdir           :only [watch-dir-ext process-last-b merge-b
                                     filter-b]]
    [hlisp.colors             :only [style pr-ok]]
    [criterium.core           :only [time-body]]
    [clojure.java.io          :only [copy file make-parents reader resource]]
    [clojure.stacktrace       :only [print-stack-trace]]
    [clojure.pprint           :only [pprint]])
  (:require
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
  (mapv #(.delete %) (filter #(.isFile %) (file-seq (file dir)))))

(defn copy-with-lastmod
  [src-file dst-file]
  (copy src-file dst-file)
  (.setLastModified dst-file (.lastModified src-file)))

(defn copy-files
  [src dest]
  (let [files  (map #(.getPath %) (filter #(.isFile %) (file-seq (file src)))) 
        outs   (map #(srcdir->outdir % src dest) files)
        srcs   (map file files)
        dsts   (map file outs)]
    (mapv make-parents dsts)
    (mapv copy-with-lastmod srcs dsts)))

(defn compile-file
  [f js-uri html-work cljs-work html-out base-uri]
  (let [path      (.getPath f)
        to-html   (if (.endsWith path ".cljs")
                    (string/replace path #"\.cljs$" ".html")
                    path)
        html-file (file (srcdir->outdir to-html html-work html-out))
        cljs-file (file (str cljs-work "/" (munge-path path) ".cljs"))] 
    (when-let [compiled (hlc/compile-file f js-uri base-uri)] 
      (mapv make-parents [html-file cljs-file])
      (spit html-file (:html compiled)) 
      (spit cljs-file (:cljs compiled)))))

(def is-file? #(.isFile %))

(defn hlisp-compile
  [{:keys [html-src cljs-src html-work cljs-work html-static html-out out-work
           outdir-out cljs-dep inc-dep ext-dep base-dir includes cljsc-opts]}]

  (delete-all html-work)
  (delete-all cljs-work)
  (delete-all html-out)
  (copy-files html-static html-out)
  (copy-files html-src html-work)
  (copy-files cljs-src cljs-work)
  (copy-files cljs-dep cljs-work)

  (let [page-files  (filter is-file? (file-seq (file html-work))) 
        incs        (mapv #(.getPath %) (filter is-file? (file-seq (file inc-dep)))) 
        exts        (mapv #(.getPath %) (filter is-file? (file-seq (file ext-dep)))) 
        env-str     (slurp (reader (resource "env.cljs")))
        env-tmp     (file cljs-work "____env.cljs")
        js-tmp      (tmpfile "____hlisp_" ".js")
        js-tmp-path (.getPath js-tmp)
        js-uri      (.getPath
                      (.relativize (.toURI (file CWD))
                                   (.toURI (file (file base-dir) "main.js"))))
        js-out      (file html-out "main.js")
        base-uri    (and (nil? (:optimizations cljsc-opts))
                         (.getPath
                           (.relativize
                             (.toURI (file CWD))
                             (.toURI (file (file base-dir) "out" "goog" "base.js"))))) 
        options     (->
                      (assoc cljsc-opts :output-to js-tmp-path)
                      (assoc :output-dir out-work)
                      (update-in [:externs] into exts))
        all-incs    (into (vec (reverse (sort incs))) includes)]
    (spit env-tmp env-str)
    (mapv #(compile-file % js-uri html-work cljs-work html-out base-uri) page-files)
    (closure/build cljs-work options)
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
    (print* (style " >> " :blue))
    (println* (-> (format "%.3f sec." (elapsed-sec hlisp-compile opts)) 
               (style :green))) 
    (run-script post-script)
    (catch Throwable e
      (println* (style "Dang!" :red))
      (print* (style (with-out-str (print-stack-trace e)) :red)))))

(defn watch-compile [{:keys [html-src cljs-src] :as opts}]
  (->>
    (merge-b (watch-dir-ext html-src "html" 100)
             (watch-dir-ext html-src "cljs" 100)) 
    (merge-b (watch-dir-ext cljs-src "cljs" 100)) 
    (process-last-b (fn [_] (compile-fancy opts))))
  (loop []
    (Thread/sleep 1000)
    (recur)))

(comment
  (.endsWith "foo.html" ".html")

  )

