(ns hlisp.core
  (:import (org.w3c.tidy Tidy) 
           (java.io StringReader StringWriter))
  (:use
    [hlisp.macros             :only [interpolate]]
    [hlisp.watchdir           :only [watch-dir-ext process-last-b merge-b
                                     filter-b]]
    [hlisp.colors             :only [style pr-ok]]
    [criterium.core           :only [time-body]]
    [pl.danieljanus.tagsoup   :only [parse tag attributes children]]
    [clojure.java.io          :only [copy file make-parents reader resource]]
    [clojure.stacktrace       :only [print-stack-trace]]
    [clojure.pprint           :only [pprint]]
    [clojure.walk             :only [postwalk]]
    [hiccup.core              :only [html]]
    [hiccup.element           :only [javascript-tag]])
  (:require
    [clojure.string           :as string]
    [cljs.closure             :as closure]))

(def CWD (System/getProperty "user.dir"))

(def hlisp-exports
  ['hlisp.env :only ['a 'abbr 'acronym 'address 'applet 'area 'article
  'aside 'audio 'b 'base 'basefont 'bdi 'bdo 'big 'blockquote 'body 'br
  'button 'canvas 'caption 'center 'cite 'code 'col 'colgroup 'command
  'data 'datalist 'dd 'del 'details 'dfn 'dir 'div 'dl 'dt 'em 'embed
  'eventsource 'fieldset 'figcaption 'figure 'font 'footer 'form 'frame
  'frameset 'h1 'h2 'h3 'h4 'h5 'h6 'head 'header 'hgroup 'hr 'html 'i
  'iframe 'img 'input 'ins 'isindex 'kbd 'keygen 'label 'legend 'li 'link
  'html-map 'mark 'menu 'html-meta 'meter 'nav 'noframes 'noscript 'object
  'ol 'optgroup 'option 'output 'p 'param 'pre 'progress 'q 'rp 'rt 'ruby
  's 'samp 'script 'section 'select 'small 'source 'span 'strike 'strong
  'style 'sub 'summary 'sup 'table 'tbody 'td 'textarea 'tfoot 'th 'thead
  'html-time 'title 'tr 'track 'tt 'u 'ul 'html-var 'video 'wbr '$text
  '$comment 'text 'pr-node 'tag 'attrs 'branch?  'children 'make-node 'dom
  'node-zip 'clone]])

(defn eagerly
  "Descend form, converting all lazy seqs into lists.
   Metadata is preserved. In the result all non-collections
   are identical? to those in the original form (as is
   their metadata). None of the collections are identical?
   even if they contains no lazy seqs."
  ;; Modified from clojure.walk/walk
  [form]
  (let [m #(with-meta % (meta form))]
    (cond (or (seq? form) (list? form))
          (m (apply list (map eagerly form)))
                          
          (vector? form)
          (m (vec (map eagerly form)))
        
          (map? form)
          (m (into (if (sorted? form) (sorted-map) {}) (map eagerly form)))

          (set? form)
          (m (into (if (sorted? form) (sorted-set) #{}) (map eagerly form)))

          :else
          form)))

(defn file-hidden?
  "Returns true if the filename begins with a dot."
  [f]
  (not= \. (first (.getName f))))

(defn file-ext
  "Returns the file extension (without the dot) or nil if none exists."
  [f]
  (let [fname (.getName f)
        i     (.lastIndexOf fname ".")
        ext   (subs fname (inc (.lastIndexOf fname ".")))]
    (when (< 0 i) (subs fname (inc i)))))

(defn file-has-ext?
  "Returns true if file f has the extenstion ext."
  [ext f]
  (= ext (file-ext f)))

(defn ext-filter
  "Given a collection of files coll and an extension string ext, returns coll
  minus any files without extension ext."
  [coll ext]
  (filter (partial file-has-ext? ext) coll))

(defn elapsed-sec
  "Given a function f and arguments, applies f to the arguments and returns the
  elapsed time in seconds."
  [f & args]
  (float (/ (first (time-body (apply f args))) 1000000000)))

(defn script?
  [form]
  (and (vector? form) (= :script (first form))))

(defn hlisp-script?
  [form]
  (and (script? form) (= "text/hlisp" (:type (second form)))))

(defn other-script?
  [form]
  (and (script? form) ((complement hlisp-script?) form)))

(defn tagsoup->hlisp
  "Given a tagsoup/hiccup data structure elem, returns the corresponding list
  of hlisp forms."
  [elem]
  (cond
    (string? elem)
    (list '$text elem)

    (hlisp-script? elem)
    (read-string (nth elem 2))
    
    :else
    (let [[t attrs & kids] elem
          tag   (symbol (name t)) 
          kids  (map tagsoup->hlisp kids)
          expr  (concat (list tag) (when (seq attrs) (list attrs)) kids)]
      (if (< 1 (count expr)) expr (first expr)))))

(defn hlisp->tagsoup
  "Given a hlisp form, returns the corresponding tagsoup/hiccup data structure."
  [form]
  (cond
    (symbol? form)
    [(keyword form) {}]

    (list? form)
    (let [[tag & tail]    form
          [attrs & kids]  (if (map? (first tail)) tail (cons {} tail))]
      (into [(keyword tag) attrs] (mapv hlisp->tagsoup kids)))

    :else
    form))

(defn pp
  [forms]
  (with-out-str (pprint forms)))

(defn pp-html
  [doctype html-str]
  (let [printer (doto (new Tidy)
                  (.setTidyMark     false)
                  (.setDocType      "omit")
                  (.setSmartIndent  true)
                  (.setShowWarnings false)
                  (.setQuiet        true))
        writer  (new StringWriter)
        reader  (new StringReader html-str)]
    (.parse printer reader writer)
    (str "<!DOCTYPE " doctype ">\n" writer)))

(defn bytag-tagsoup
  [tag page]
  (if (vector? page)
    (let [[mytag attrs & kids] page]
      (if (= tag mytag)
        page
        (first (remove nil? (map (partial bytag-tagsoup tag) kids)))))
    nil))

(defn empty-tagsoup
  [tag page]
  (if (vector? page)
    (let [[mytag attrs & kids] page]
      (if (= tag mytag)
        [mytag attrs]
        (into [mytag attrs] (mapv (partial empty-tagsoup tag) kids))))
    page))

(defn append-tagsoup
  [tag elem page]
  (if (vector? page)
    (let [[mytag attrs & kids] page]
      (if (= tag mytag)
        (conj page elem)
        (into [mytag attrs] (mapv (partial append-tagsoup tag elem) kids))))
    page))

(defn wrap-body
  [page]
  (map
    (fn [form]
      (if (and (list? form) (= 'body (first form)))
        (let [[tag & tail]    form
              [attrs & kids]  (if (map? (first tail)) tail (cons {} tail))]
          (list tag attrs (list 'script {:type "text/hlisp"} (pp (cons 'do kids))))) 
        form))
    page))

(defn parse-hcljs
  [s]
  (let [page    (read-string (str "(" s ")")) 
        script  [:script {:type "text/hlisp"} (apply str (mapv pp (butlast page)))]
        html    (hlisp->tagsoup (eagerly (wrap-body (last page))))]
    (->> html
      (append-tagsoup :head script))))

(defn hcljs-tagsoup->html
  [page]
  (let [html-str (append-tagsoup :body "<!--__HLISP__-->" page)]
    (pp-html "html" (html html-str))))

(defn extract-cljs-script
  "Given a tagsoup/hiccup data structure page, returns a list of hlisp forms
  read from the <script type=\"text/hlisp\"> element in the page <head>."
  [page]
  (let [elem      (->> (children page)
                    (filter #(= :head (first %)))
                    (first)
                    (children)
                    (filter #(= :script (first %)))
                    (filter #(= "text/hlisp" (:type (second %))))
                    (first))]
    (read-string (str "(" (nth elem 2) ")"))))

(defn process-cljs-body-scripts
  [body]
  (postwalk
    #(if (hlisp-script? %) (assoc-in % [2] (str "(do " (nth % 2) ")")) %)
    body))

(defn process-other-body-scripts
  [body]
  (postwalk
    #(if (other-script? %) (assoc-in % [2] "") %)
    body))

(defn extract-cljs-body
  "Given a tagsoup/hiccup data structure page, returns a list of hlisp forms
  corresponding to the contents of the page <body>."
  [page]
  (->> (children page)
    (filter #(= :body (first %)))
    (first)
    (process-cljs-body-scripts)
    (process-other-body-scripts)
    (drop 2)
    (mapv tagsoup->hlisp)
    (interpolate)
    (first)))

(defn add-hlisp-uses
  [[_ nm & forms]]
  (let [parts (group-by #(= :use (first %)) forms)
        uses  (concat (or (first (get parts true)) (list :use)) (list hlisp-exports)) 
        other (get parts false)] 
    (list* 'ns nm uses other)))

(defn build-cljs-str
  "Given lists of hlisp forms script-forms and body-forms (from the <script>
  and <body> tags in the page, resp.), returns a string of cljs source for
  the page."
  [script-forms body-forms]
  (let [[ns-decl & sforms] script-forms
        wrapped (pp (list (symbol "hlisp.env/init") body-forms)) 
        s1      (pr-str (add-hlisp-uses ns-decl))
        s2      (string/join "\n" (map pr-str sforms))
        s3      (str "(defn ^:export hlispinit []\n" wrapped ")")]
    (string/join "\n" [s1 s2 s3])))

(defn build-html-str
  "Given html content html-in, the deployed location of main.js js-out, and
  the namespace of the page page-ns, returns the processed html content."
  [html-in js-out page-ns]
  (let [[dummy pad] (re-find #"( *)<!--__HLISP__-->" html-in)
        s1      [:script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true"]
        s2      [:script {:type "text/javascript" :src js-out}]
        s3      [:script {:type "text/javascript"} (str page-ns ".hlispinit()")]
        scripts (string/join "\n" (map #(str pad %) [(html s1)
                                                     (html s2)
                                                     (html s3)]))]
    (string/replace-first html-in dummy scripts)))

(defn prepare-compile
  [js-out html-in html-out cljs-out]
  (let [parsed (parse html-in)] 
    (mapv make-parents [js-out html-out cljs-out])
    (if-let [sforms (seq (extract-cljs-script parsed))]
      (let [page-ns   (second (first sforms))
            bforms    (extract-cljs-body parsed)
            html-str  (build-html-str (slurp html-in) js-out page-ns)
            cljs-str  (build-cljs-str sforms bforms)]
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

(defn is-file? [f] (.isFile f))
(defn html-file? [f] (and (is-file? f) (= "html" (file-ext f))))
(defn cljs-file? [f] (and (is-file? f) (= "cljs" (file-ext f))))
(defn other-file? [f] (and (is-file? f) (not (or (html-file? f) (cljs-file? f)))))

(defn delete-all
  [dir]
  (mapv #(.delete %) (filter #(.isFile %) (file-seq (file dir)))))

(defn prepare-hcljs
  [html-src]
  (let [html-files (file-seq (file html-src))
        hcljs-ins   (->> html-files
                      (filter cljs-file?)
                      (map #(.getPath %)))
        hcljs-srcs  (->> hcljs-ins
                      (map #(string/replace-first % #"\.cljs$" ".html")))
        process     (fn [in src]
                      (make-parents src)
                      (spit src (hcljs-tagsoup->html (parse-hcljs (slurp in)))))]
    (mapv process hcljs-ins hcljs-srcs)))

(defn copy-files
  [src dest]
  (let [files  (map #(.getPath %) (filter #(.isFile %) (file-seq (file src)))) 
        outs   (map #(srcdir->outdir % src dest) files)]
    (mapv make-parents (map file outs))
    (mapv #(copy (file %1) (file %2)) files outs)))

(defn hlisp-prepare
  [html-src cljs-src html-work cljs-work] 
  (copy-files html-src html-work)
  (copy-files cljs-src cljs-work))

(defn hlisp-compile
  [{:keys [html-src cljs-src html-work cljs-work html-out
           cljs-dep inc-dep ext-dep base-dir includes cljsc-opts]}]
  (delete-all html-work)
  (delete-all cljs-work)
  (delete-all html-out)
  (hlisp-prepare html-src cljs-src html-work cljs-work)
  (prepare-hcljs html-work)
  (copy-files cljs-dep cljs-work)
  (let [html-files  (file-seq (file html-work))
        cljs-files  (file-seq (file cljs-work))
        html-ins    (->> html-files
                      (filter html-file?)
                      (map #(.getPath %)))
        other-ins   (->> html-files
                      (filter other-file?)
                      (map #(.getPath %)))
        incs        (mapv #(.getPath %) (filter is-file? (file-seq (file inc-dep)))) 
        exts        (mapv #(.getPath %) (filter is-file? (file-seq (file ext-dep)))) 
        html-outs   (map #(srcdir->outdir % html-work html-out) html-ins)
        cljs-outs   (map #(file cljs-work (str (munge-path %) ".cljs")) html-ins)
        other-outs  (map #(srcdir->outdir % html-work html-out) other-ins)
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
    (mapv (partial prepare-compile js-uri) html-ins html-outs cljs-outs)
    (mapv #(copy (file %1) (file %2)) other-ins other-outs)
    (closure/build cljs-work options)
    (spit js-out (string/join "\n" (map slurp (conj all-incs js-tmp-path))))
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
             (watch-dir-ext html-src "cljs" 100)) 
    (merge-b (watch-dir-ext cljs-src "cljs" 100)) 
    (process-last-b (fn [_] (compile-fancy opts))))
  (loop []
    (Thread/sleep 1000)
    (recur)))

(comment


  )

