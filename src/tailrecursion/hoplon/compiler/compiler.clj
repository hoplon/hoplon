(ns tailrecursion.hoplon.compiler.compiler
  (:require
    [clojure.walk                           :refer  [stringify-keys]]
    [clojure.java.io                        :refer  [file make-parents]]
    [clojure.pprint                         :refer  [pprint]]
    [clojure.zip                            :as     zip]
    [clojure.string                         :as     string]
    [cljs.compiler                          :as     cljsc]
    [tailrecursion.hoplon.compiler.tagsoup  :as     ts]))

(def ^:dynamic *current-file* nil)
(def ^:dynamic *printer*      prn)

(def html-tags
  '[a abbr acronym address applet area article
    aside audio b base basefont bdi bdo big blockquote body br
    button canvas caption center cite code col colgroup command
    data datalist dd del details dfn dir div dl dt em embed
    eventsource fieldset figcaption figure font footer form frame
    frameset h1 h2 h3 h4 h5 h6 head header hgroup hr html i
    iframe img input ins isindex kbd keygen label legend li link
    html-map mark menu html-meta meter nav noframes noscript object
    ol optgroup option output p param pre progress q rp rt ruby
    s samp script section select small source span strike strong
    style sub summary sup table tbody td textarea tfoot th thead
    html-time title tr track tt u ul html-var video wbr $text
    $comment])

(defn relative-to [base f] (.relativize (.toURI base) (.toURI f)))

(defn up-parents [f base & parts]
  (->> (file f)
    (relative-to (file base))
    .getPath
    file
    (iterate #(.getParentFile %))
    (take-while identity)
    butlast
    (map (constantly ".."))
    (concat (reverse parts))
    reverse
    (apply file)
    .getPath))

(defn srcdir->outdir [fname srcdir outdir]
  (.getPath (file outdir (.getPath (relative-to (file srcdir) (file fname))))))

(defn munge-path [path]
  (-> (str "__" path) (string/replace "_" "__") (string/replace "/" "_")))

(def hoplon-exports
  ['tailrecursion.hoplon.env :only
   (into html-tags '[text pr-node tag attrs branch? children make-node dom node-zip clone])])

(defn clj->css [forms]
  (let [[selectors properties]
        (apply map vector (partition 2 (partition-by map? forms)))
        sel-str   (fn [sels]
                    (->> sels
                      (map #(string/join " " (map name %)))
                      (string/join ",\n")))
        prop-str  (fn [[props & _]]
                    (let [p (->> props
                              (stringify-keys) 
                              (map (comp (partial str "  ")
                                         (partial string/join ": ")))
                              (string/join ";\n"))]
                      (str " {\n" p ";\n}\n")))
        sel-strs  (map sel-str selectors)
        prop-strs (map prop-str properties)]
    (->> (map str sel-strs prop-strs) (string/join "\n"))))

(defn tree-update-in
  [root pred f]
  (let [dz      (zip/seq-zip root)
        update  (fn [loc]
                  (let [n (zip/node loc)]
                    (if (pred n) (zip/replace loc (f n)) loc)))]
    (loop [loc dz]
      (if (zip/end? loc)
        (zip/root loc)
        (recur (zip/next (update loc)))))))

(defn tree-update-splicing [root pred f]
  (letfn [(t-u-s [r p f]
            (cond (p r)     (f r)
                  (seq? r)  (->> r (map #(t-u-s % p f)) (apply concat) list)
                  :else     (list r)))]
    (apply concat (t-u-s root pred f))))

(defn style [[_ & forms]]
  (if (vector? (first forms))
    (list 'style {:type "text/css"} (clj->css forms))
    (list* 'style forms)))

(defn process-styles
  [root]
  (tree-update-in root #(and (seq? %) (= 'style (first %))) style))

(defn is-tag? [tag form]
  (and (seq? form) (= tag (first form))))

(defn filter-tag [tag forms]
  (filter (partial is-tag? tag) forms))

(defn prepend-children
  [[tag & tail] newkids]
  (let [a (first tail)]
    (list* tag (if (map? a)
                 (list* a (concat newkids (rest tail)))
                 (list* {} (concat newkids tail))))))

(defn add-hoplon-uses
  [[_ nm & forms]]
  (let [parts (group-by #(= :use (first %)) forms)
        uses  (concat (or (first (get parts true)) (list :use)) (list hoplon-exports)) 
        other (get parts false)] 
    (list* 'ns nm uses other)))

(defn compile-forms [html-forms js-uri]
  (let [body-noinc    (first (filter-tag 'body html-forms))
        forms-noinc   (drop (if (map? (second body-noinc)) 2 1) body-noinc) 
        bhtml         (map ts/pedanticize (rest forms-noinc))
        body    (first (filter-tag 'body html-forms))
        battr   (let [a (second body)] (if (map? a) a {}))
        forms   (drop (if (map? (second body)) 2 1) body) 
        nsdecl  (add-hoplon-uses (first forms)) 
        nsname  (cljsc/munge (second nsdecl)) 
        emptyjs (str "(function(node) {"
                     " while (node.hasChildNodes())"
                     " node.removeChild(node.lastChild)"
                     " })(document.body);")
        s-empty (list 'script {:type "text/javascript"} emptyjs)
        s-main  (list 'script {:type "text/javascript" :src js-uri})
        s-nodep (list 'script {:type "text/javascript"}
                      "var CLOSURE_NO_DEPS = true;")
        s-goog  (list 'script {:type "text/javascript"}
                      (str "goog.require('" nsname "');"))
        s-init  (list 'script {:type "text/javascript"}
                      (str nsname ".hoploninit();"))
        scripts (list s-empty s-nodep s-main s-init)
        bnew    (list* 'body battr (concat bhtml scripts))
        cljs    (concat
                  (list nsdecl)
                  (list
                    (list 'defn (symbol "^:export") 'hoploninit []
                          (list (symbol "tailrecursion.hoplon.env/init") (vec (drop 1 forms)))))) 
        cljsstr  (string/join "\n" (map #(with-out-str (*printer* %)) cljs))
        html    (replace {body bnew} html-forms)
        htmlstr (ts/pp-forms "html" html)]
    {:html htmlstr :cljs cljsstr}))

(defn move-cljs-to-body
  [[[first-tag & _ :as first-form] & more :as forms]]
  (case first-tag
    ns    (let [html-forms        (process-styles (last forms)) 
                [nsdecl & exprs]  (butlast forms)
                cljs-forms        (list*
                                    nsdecl
                                    (list (list* 'do (concat exprs (list 'nil)))))
                body              (first (filter-tag 'body html-forms)) 
                bnew              (prepend-children body cljs-forms)]
            (replace {body bnew} html-forms))
    html  first-form
    (throw (Exception. "First tag is not HTML or namespace declaration."))))

(defn compile-ts [html-ts js-uri]
  (compile-forms (move-cljs-to-body (ts/tagsoup->hoplon html-ts)) js-uri))

(defn compile-string [html-str js-uri]
  (compile-ts (ts/parse-string html-str) js-uri))

(defn compile-file
  [f js-uri]
  (let [read-all  #(read-string (str "(" (slurp %) ")"))
        do-move   #(move-cljs-to-body (read-all %))
        do-html   #(compile-string (slurp %) js-uri)
        do-cljs   #(compile-forms identity (do-move %) js-uri)
        domap     {"html" do-html "cljs" do-cljs}
        doit      #(domap (last (re-find #"[^.]+\.([^.]+)$" %)))]
    (binding [*current-file* f] ((doit (.getPath f)) f))))

(defn compile-dir
  [js-file srcdir cljsdir htmldir]
  (let [to-html     #(let [path (.getPath %)]
                       (if (.endsWith path ".cljs")
                         (str (subs path 0 (- (count path) 5)) ".html")
                         path))
        ->htmldir   #(file (srcdir->outdir (to-html %) srcdir htmldir))
        ->cljsdir   #(file cljsdir (str (munge-path (.getPath %)) ".cljs"))
        src?        #(and (.isFile %) (re-find #"\.(html|cljs)$" (.getName %)))
        srcs        (into [] (filter src? (file-seq (file srcdir)))) 
        js-uris     (mapv #(up-parents % srcdir (.getName js-file)) srcs)
        compiled    (mapv compile-file srcs js-uris)
        html-outs   (mapv ->htmldir srcs)
        cljs-outs   (mapv ->cljsdir srcs)
        write       #(spit (doto %1 make-parents) %2)
        write-files (fn [h c {:keys [html cljs]}] (write h html) (write c cljs))]
    (doall (map write-files html-outs cljs-outs compiled))))

(comment
  (binding [*printer* pprint]
    (println (:cljs (-> (compile-file (file "test/index.html") "main.js"))))) 
  )
