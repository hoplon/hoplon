(ns hlisp.core
  (:use
    [criterium.core         :only [time-body]]
    [hlisp.watchdir         :only [watch-dir-ext process-last-b merge-b
                                   filter-b]]
    [hlisp.colors           :only [style pr-ok]]
    [pl.danieljanus.tagsoup :only [parse tag attributes children]]
    [clojure.java.io        :only [copy file reader resource]]
    [clojure.stacktrace     :only [print-stack-trace]]
    [clojure.pprint         :only [pprint]]
    [hiccup.core            :only [html]]
    [hiccup.element         :only [javascript-tag]])
  (:require
    [clojure.string         :as string]
    [cljs.closure           :as closure]))

(def CWD (System/getProperty "user.dir"))

(defn file-hidden? [f]
  (not= \. (first (.getName f))))

(defn file-ext [f]
  (let [fname (.getName f)
        i     (.lastIndexOf fname ".")
        ext   (subs fname (inc (.lastIndexOf fname ".")))]
    (when (< 0 i) (subs fname (inc i)))))

(defn file-has-ext? [ext f]
  (= ext (file-ext f)))

(defn ext-filter [coll ext]
  (filter (partial file-has-ext? ext) coll))

(defn elapsed-sec [f & args]
  (float (/ (first (time-body (apply f args))) 1000000000)))

(defn tagsoup->hlisp [elem]
  (if (string? elem)
    (list '$text elem)
    (let [[t attrs & kids] elem
          tag   (symbol (name t)) 
          kids  (map tagsoup->hlisp kids)]
      (list* tag attrs kids))))

(defn extract-cljs-script [page]
  (let [elem      (->> (children page)
                    (filter #(= :head (first %)))
                    (first)
                    (children)
                    (filter #(= :script (first %)))
                    (filter #(= "text/hlisp" (:type (second %))))
                    (first))]
    (read-string (str "(" (nth elem 2) ")"))))

(defn extract-cljs-body [page]
  (->> (children page)
    (filter #(= :body (first %)))
    (first)
    (drop 2)
    (mapv tagsoup->hlisp)))

(defn build-cljs-str [script-forms body-forms prelude]
  (let [[ns-decl & sforms] script-forms
        wrapped (list (symbol "hlisp.env/init") body-forms)
        s1      (pr-str ns-decl)
        s2      (string/join "\n" (map pr-str sforms))
        s3      (str "(defn ^:export hlispinit []\n" (pr-str wrapped) ")")]
    (string/join "\n" [s1 (string/trim prelude) s2 s3])))

(defn build-html-str [html-in js-out page-ns]
  (let [[dummy pad] (re-find #"( *)<!--__HLISP__-->" html-in)
        s1      [:script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true"]
        s2      [:script {:type "text/javascript" :src js-out}]
        s3      [:script {:type "text/javascript"} (str page-ns ".hlispinit()")]
        scripts (string/join "\n" (map #(str pad %) [(html s1)
                                                     (html s2)
                                                     (html s3)]))]
    (string/replace-first html-in dummy scripts)))

(defn prepare-compile [prelude js-out html-in html-out cljs-out]
  (let [parsed (parse html-in)] 
    (if-let [sforms (seq (extract-cljs-script parsed))]
      (let [page-ns   (second (first sforms))
            bforms    (extract-cljs-body parsed)
            html-str  (build-html-str (slurp html-in) js-out page-ns)
            cljs-str  (build-cljs-str sforms bforms prelude)]
        (spit cljs-out cljs-str) 
        (spit html-out html-str)) 
      (copy html-in html-out))))

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

(defn tmp-cljs-file? [f]
  (.startsWith (.getName (file f)) "____"))

(defn hlisp-compile
  [{:keys [html-src cljs-src html-out base-dir prelude includes cljsc-opts]}]
  (let [html-ins    (->>
                      (file html-src)
                      (file-seq)
                      (filter #(= "html" (file-ext %)))
                      (map #(.getPath %)))
        stale       (->>
                      (file cljs-src)
                      (file-seq)
                      (filter tmp-cljs-file?))
        html-outs   (map #(srcdir->outdir % html-src html-out) html-ins)
        cljs-outs   (map #(file cljs-src (str (munge-path %) ".cljs")) html-ins)
        prelude-str (slurp (reader (resource "prelude.cljs")))
        env-str     (slurp (reader (resource "env.cljs")))
        env-tmp     (file cljs-src "____env.cljs")
        js-tmp      (tmpfile "____hlisp_" ".js")
        js-tmp-path (.getPath js-tmp)
        js-uri      (.getPath
                      (.relativize (.toURI (file CWD))
                                   (.toURI (file (file base-dir) "main.js"))))
        js-out      (file html-out "main.js")
        options     (assoc cljsc-opts :output-to js-tmp-path)]
    (mapv #(.delete %) stale) 
    (spit env-tmp env-str)
    (mapv (partial prepare-compile prelude-str js-uri) html-ins html-outs cljs-outs)
    (closure/build cljs-src options)
    (spit js-out (string/join "\n" (map slurp (conj includes js-tmp-path))))
    (.delete js-tmp)))

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
             (watch-dir-ext cljs-src "cljs" 100)) 
    (filter-b (complement tmp-cljs-file?))
    (process-last-b (fn [_] (compile-fancy opts))))
  (loop []
    (Thread/sleep 1000)
    (recur)))

(comment

  clojure.contrib.map-utils/deep-merge-with 

  (munge-path "src/html/index.html")

  (doit) 


  )

