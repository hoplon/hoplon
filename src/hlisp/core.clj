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
  (str outdir "/" (subs fname (inc (count srcdir)))))

(defn delete-all
  [dir]
  (mapv #(.delete %) (filter #(.isFile %) (file-seq (file dir)))))

(defn copy-files
  [src dest]
  (let [files  (map #(.getPath %) (filter #(.isFile %) (file-seq (file src)))) 
        outs   (map #(srcdir->outdir % src dest) files)]
    (mapv make-parents (map file outs))
    (mapv #(copy (file %1) (file %2)) files outs)))

(defn compile-file
  [f js-uri html-work cljs-work html-out]
  (let [path      (.getPath f)
        to-html   (if (re-matches #"\.cljs$" path)
                    (string/replace path #"\.cljs$" ".html")
                    path)
        to-cljs   (str cljs-work "/" (munge-path path) ".cljs")] 
    (when-let [compiled (hlc/compile-file f)] 
      (spit (file (srcdir->outdir to-html html-work html-out)) (:html compiled)) 
      (spit (file to-cljs) (:cljs compiled)))))

(defn hlisp-compile
  [{:keys [html-src cljs-src html-work cljs-work html-out
           cljs-dep inc-dep ext-dep base-dir includes cljsc-opts]}]

  (delete-all html-work)
  (delete-all cljs-work)
  (delete-all html-out)
  (copy-files html-src html-work)
  (copy-files cljs-src cljs-work)
  (copy-files cljs-dep cljs-work)

  (let [page-files  (file-seq (file html-work))
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
        options     (-> (assoc cljsc-opts :output-to js-tmp-path)
                      (update-in [:externs] into exts))
        all-incs    (into (vec (reverse (sort incs))) includes)]
    (spit env-tmp env-str)
    (mapv #(compile-file % js-uri html-work cljs-work html-out) page-files)
    (closure/build cljs-work options)
    (spit js-out (string/join "\n" (map slurp (conj all-incs js-tmp-path))))
    (.delete js-tmp)))

(defn elapsed-sec
  "Given a function f and arguments, applies f to the arguments and returns the
  elapsed time in seconds."
  [f & args]
  (float (/ (first (time-body (apply f args))) 1000000000)))

(defn compile-fancy [opts]
  (print (style (str (java.util.Date.) " << ") :blue))
  (print (style "compiling" :bold-blue))
  (print (style " >> " :blue))
  (flush)
  (try
    (let [elapsed (elapsed-sec hlisp-compile opts)]
      (println (-> (format "%.3f sec." elapsed) (style :green)))
      (flush)) 
    (catch Throwable e
      (println (style "Dang!" :red))
      (print (style (with-out-str (print-stack-trace e)) :red))
      (flush))))

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


  )

