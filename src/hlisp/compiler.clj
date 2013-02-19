(ns hlisp.compiler
  (:use
    [clojure.walk       :only [stringify-keys]]
    [hlisp.util.re-map  :only [re-map]]
    [clojure.java.io    :only [file]]
    [clojure.pprint     :only [pprint]])
  (:require
    [clojure.zip        :as   zip]
    [clojure.string     :as   string]
    [hlisp.tagsoup      :as   ts]))

(def ^:dynamic *current-file* nil)

(def html-tags
  ['a 'abbr 'acronym 'address 'applet 'area 'article
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
   '$comment])

(def hlisp-exports
  ['hlisp.env :only
   (into html-tags
         ['text 'pr-node 'tag 'attrs 'branch? 'children 'make-node 'dom
          'node-zip 'clone])])

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
                      (str " {\n" p ";\n}\n")))]
    (->>
      (map str (map sel-str selectors) (map prop-str properties))
      (string/join "\n"))))

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

(defn tree-update-splicing
  ([root pred f]
   (apply concat (tree-update-splicing root pred f ::doit))) 
  ([root pred f _]
   (cond
     (pred root)
     (f root)

     (seq? root)
     (list (apply concat (map #(tree-update-splicing % pred f ::doit) root)))

     :else
     (list root))))

(defn style
  [[_ & forms]]
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

(defn guess-tag
  [tag kids]
  (cond
    (some #(or (= 'option (first %)) (= 'optgroup (first %))) kids) 'select
    :else 'div))

(defn htmlize
  [[tag & tail :as form]]
  (if (map? (first tail))
    (let [attr (first tail)
          kids (mapv htmlize (rest tail))]
      (if (some #{tag} html-tags)
        (list* tag attr kids)
        (list* (guess-tag tag kids) attr kids)))
    form))

(defn process-includes
  [root]
  (let [cur-dir (.getParentFile *current-file*)]
    (tree-update-splicing
      root
      (fn [x]
        (when (and (seq? x) (< 1 (count x)))
          (let [[tag attr & _] x]
            (when (map? attr)
              (let [{:keys [type src]} attr]
                (and (= 'include tag) (= "text/hlisp" type) (string? src)))))))
      (fn [[_ {:keys [src]} & _]]
        (let [inc-file          (.getCanonicalFile (file cur-dir src))
              [nsdecl & forms]  (read-string (str "(" (slurp inc-file) ")"))
              do-expr           (list* 'do (concat forms (list 'nil)))] 
          (list nsdecl do-expr))))))

(defn add-hlisp-uses
  [[_ nm & forms]]
  (let [parts (group-by #(= :use (first %)) forms)
        uses  (concat (or (first (get parts true)) (list :use)) (list hlisp-exports)) 
        other (get parts false)] 
    (list* 'ns nm uses other)))

(defn compile-forms [proc html-forms js-uri base-uri]
  (let [body-noinc    (first (filter-tag 'body html-forms))
        forms-noinc   (drop (if (map? (second body-noinc)) 2 1) body-noinc) 
        bhtml         (map (comp htmlize ts/pedanticize) (rest forms-noinc))
        html-forms    (proc html-forms)
        body    (first (filter-tag 'body html-forms))
        battr   (let [a (second body)] (if (map? a) a {}))
        forms   (drop (if (map? (second body)) 2 1) body) 
        nsdecl  (add-hlisp-uses (first forms)) 
        nsname  (second nsdecl)
        emptyjs (string/join "\n" ["(function(node) {"
                                   "  while (node.hasChildNodes())"
                                   "    node.removeChild(node.lastChild);"
                                   "})(document.body);"])
        s-empty (list 'script {:type "text/javascript"} emptyjs)
        s-base  (list 'script {:type "text/javascript" :src base-uri})
        s-main  (list 'script {:type "text/javascript" :src js-uri})
        s-nodep (list 'script {:type "text/javascript"}
                      "var CLOSURE_NO_DEPS = true;")
        s-goog  (list 'script {:type "text/javascript"}
                      (str "goog.require('" nsname "');"))
        s-init  (list 'script {:type "text/javascript"}
                      (str nsname ".hlispinit();"))
        scripts (if base-uri
                  (list s-empty s-base s-main s-goog s-init)
                  (list s-empty s-nodep s-main s-init))
        bnew    (list* 'body battr (concat bhtml scripts))
        cljs    (concat
                  (list nsdecl)
                  (list
                    (list 'defn (symbol "^:export") 'hlispinit []
                          (list (symbol "hlisp.env/init") (vec (drop 1 forms)))))) 
        cljsstr (string/join "\n" (map #(with-out-str (pprint %)) cljs)) 
        html    (replace {body bnew} html-forms)
        htmlstr (ts/pp-html "html" (ts/html (ts/hlisp->tagsoup html)))]
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

(defn compile-ts [html-ts js-uri base-uri]
  (compile-forms process-includes (first (ts/tagsoup->hlisp html-ts)) js-uri base-uri))

(defn compile-string [html-str js-uri base-uri]
  (compile-ts (ts/parse-string html-str) js-uri base-uri))

(defn compile-file
  [f js-uri base-uri]
  (let [doit
        (re-map
          #"\.html$" #(compile-string (slurp %) js-uri base-uri)
          #"\.cljs$" #(compile-forms
                        identity
                        (move-cljs-to-body
                          (read-string (str "(" (slurp %) ")")))
                        js-uri
                        base-uri)
          #".*"      (constantly nil))]
    (binding [*current-file* f]
      ((doit (.getPath f)) f))))

(comment

  )
