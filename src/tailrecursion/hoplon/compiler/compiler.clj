(ns tailrecursion.hoplon.compiler.compiler
  (:require
    [clojure.walk                           :refer  [stringify-keys]]
    [clojure.java.io                        :refer  [file make-parents]]
    [clojure.pprint                         :as     pprint]
    [clojure.zip                            :as     zip]
    [clojure.string                         :as     string]
    [cljs.compiler                          :as     cljsc]
    [tailrecursion.javelin                  :refer  [make-require make-require-macros]]
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
    $comment spliced])

(defn relative-to [base f] (.relativize (.toURI base) (.toURI f)))

#_(defn up-parents [f base & parts]
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

(defn up-parents [path name]
  (let [[f & dirs] (string/split path #"/")]
    (->> [name] (concat (repeat (count dirs) "../")) (apply str))))

(defn srcdir->outdir [fname srcdir outdir]
  (.getPath (file outdir (.getPath (relative-to (file srcdir) (file fname))))))

(defn munge-path [path]
  (-> (str "__" path) (string/replace "_" "__") (string/replace "/" "_")))

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

(defn compile-forms [html-forms js-file]
  (let [body*   (first (filter-tag 'body html-forms))
        forms*  (drop (if (map? (second body*)) 2 1) body*) 
        bhtml   (map ts/pedanticize (rest forms*))
        body    (first (filter-tag 'body html-forms))
        battr   (let [a (second body)] (if (map? a) a {}))
        forms   (drop (if (map? (second body)) 2 1) body) 
        outpath (str (second (first forms)))
        js-uri  (up-parents outpath (.getName js-file))
        nsdecl  (let [n (first forms)]
                  (concat (take 1 n) [(munge (second n))] (drop 2 n))) 
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
                          (list (symbol "tailrecursion.hoplon/init") (vec (drop 1 forms)))))) 
        cljsstr  (string/join "\n" (map #(with-out-str (*printer* %)) cljs))
        html    (replace {body bnew} html-forms)
        htmlstr (ts/pp-forms "html" html)]
    {:html htmlstr :cljs cljsstr :file outpath}))

(defn make-nsdecl
  [[_ ns-sym & forms]]
  (let [ns-syms '#{tailrecursion.hoplon tailrecursion.javelin}
        rm?     #(or (contains? ns-syms %) (and (seq %) (contains? ns-syms (first %))))
        mk-req  #(concat (remove rm? %2) (map %1 ns-syms))
        clauses (->> (tree-seq list? seq forms) (filter list?) (group-by first))
        combine #(mapcat (partial drop 1) (% clauses))
        reqs    `(:require ~@(mk-req make-require (combine :require)))
        macros  `(:require-macros ~@(mk-req make-require-macros (combine :require-macros)))]
    `(~'ns ~ns-sym ~reqs ~macros)))

(defn move-cljs-to-body
  [[[first-tag & _ :as first-form] & more :as forms]]
  (case first-tag
    page  (let [html-forms      (process-styles (last forms)) 
                [page & exprs]  (butlast forms)
                cljs-forms      `(~(make-nsdecl page) (do ~@exprs nil))
                body            (first (filter-tag 'body html-forms)) 
                bnew            (prepend-children body cljs-forms)]
            (replace {body bnew} html-forms))
    html  first-form
    (throw (Exception. "First tag is not page declaration."))))

(defn compile-ts [html-ts js-file]
  (compile-forms (move-cljs-to-body (ts/tagsoup->hoplon html-ts)) js-file))

(defn compile-string [html-str js-file]
  (compile-ts (ts/parse-string html-str) js-file))

(defn pp [form] (pprint/write form :dispatch pprint/code-dispatch))

(defn compile-file
  [f js-file cljsdir htmldir & {:keys [opts]}]
  (let [read-all  #(read-string (str "(" (slurp %) ")"))
        do-move   #(move-cljs-to-body (read-all %))
        do-html   #(compile-string (slurp %) js-file)
        do-cljs   #(compile-forms (do-move %) js-file)
        domap     {"html" do-html "cljs" do-cljs}
        doit      #(domap (last (re-find #"[^.]+\.([^.]+)\.hl$" %)))]
    (binding [*current-file*  f
              *printer*       (if (:pretty-print opts) pp prn)]
      (if-let [compile (doit (.getPath f))]
        (let [compiled (compile f)
              cljs-out (file cljsdir (str (munge-path (.getPath f)) ".cljs"))
              html-out (file htmldir (:file compiled))
              write    #(spit (doto %1 make-parents) %2)]
          (doall (map write [cljs-out html-out] ((juxt :cljs :html) compiled))))))))

(comment
  (binding [*printer* pprint]
    (println (:cljs (-> (compile-file (file "test/index.html") "main.js"))))) 
  )
