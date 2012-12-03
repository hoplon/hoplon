(ns hlisp.compiler
  (:use
    [hlisp.util.re-map  :only [re-map]]
    [clojure.java.io    :only [file]]
    [clojure.pprint     :only [pprint]])
  (:require
    [clojure.string     :as   string]
    [hlisp.tagsoup      :as   ts]))

(def hlisp-exports
  ['hlisp.env :only
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
    '$comment 'text 'pr-node 'tag 'attrs 'branch?  'children 'make-node 'dom
    'node-zip 'clone]])

(defn add-hlisp-uses
  [[_ nm & forms]]
  (let [parts (group-by #(= :use (first %)) forms)
        uses  (concat (or (first (get parts true)) (list :use)) (list hlisp-exports)) 
        other (get parts false)] 
    (list* 'ns nm uses other)))

(defn compile-forms [html-forms js-uri base-uri]
  (let [body    (first (filter #(and (seq? %) (= 'body (first %))) html-forms))
        battr   (let [a (second body)] (if (map? a) a {}))
        forms   (drop (if (map? (second body)) 2 1) body) 
        nsdecl  (add-hlisp-uses (first forms)) 
        nsname  (second nsdecl)
        scripts (if base-uri
                  (list (list 'script {:type "text/javascript" :src base-uri})
                        (list 'script {:type "text/javascript" :src js-uri})
                        (list 'script {:type "text/javascript"}
                              (str "goog.require('" nsname "');"))
                        (list 'script {:type "text/javascript"}
                              (str nsname ".hlispinit()")))
                  (list (list 'script {:type "text/javascript"}
                              "var CLOSURE_NO_DEPS = true")
                        (list 'script {:type "text/javascript" :src js-uri})
                        (list 'script {:type "text/javascript"}
                              (str nsname ".hlispinit()"))))
        bnew    (list* 'body battr scripts)
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
    ns    (let [html-forms  (last forms)
                cljs-forms  (butlast forms)
                body        (first (filter #(and (seq? %) (= 'body (first %))) html-forms)) 
                bnew        (let [a (second body)]
                              (list*
                                'body
                                (if (map? a)
                                  (list* a (concat cljs-forms (drop 2 body)))
                                  (list* {} (concat cljs-forms (rest body))))))]
            (replace {body bnew} html-forms))
    html  first-form
    (throw (Exception. "First tag is not HTML or namespace declaration."))))

(defn compile-ts [html-ts js-uri base-uri]
  (compile-forms (first (ts/tagsoup->hlisp html-ts)) js-uri base-uri))

(defn compile-string [html-str js-uri base-uri]
  (compile-ts (ts/parse-string html-str) js-uri base-uri))

(defn compile-file
  [f js-uri base-uri]
  (let [doit
        (re-map
          #"\.html$" #(compile-string (slurp %) js-uri base-uri)
          #"\.cljs$" #(compile-forms
                        (move-cljs-to-body
                          (read-string (str "(" (slurp %) ")")))
                        js-uri
                        base-uri)
          #".*"      (constantly nil))]
    ((doit (.getPath f)) f)))

(comment

  (compile-file (file "asdfasdf") "/main.js")

  (pprint
    (ts/parse (file "../hlisp-starter/src/html/index.html")) 
    )

  (println
    (:cljs (compile-file (file "test/html/index.html") "/main.js")) 
    )

  (println
    (:html (compile-file (file "test/html/foo.cljs") "/main.js")) 
    )

  )
