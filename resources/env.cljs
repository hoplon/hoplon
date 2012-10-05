(ns hlisp.env
  (:require
    [jayq.core        :as jq]
    [jayq.util        :as ju])
  (:use
    [clojure.string   :only [join]]))

(comment

  ;; In the cljs repl you can do things like these and see the results in both
  ;; the repl and the browser console.

  (in-ns 'hlisp.env)

  (div
    ($comment "hey there")
    (p ($text "foo"))
    (p ($text "bar")))

  ;; (div ($comment "hey there") (p ($text "foo")) (p ($text "bar")))
  ;; <div>
  ;;   <!--hey there-->
  ;;   <p>foo</p>
  ;;   <p>bar</p>
  ;; </div>

  (defn foo [x]
    (div
      (h1 ($text "Title"))
      x))

  ;; #<function foo(x){ ... }>

  (foo (p ($text "hello world")))
  
  ;; (div (h1 ($text "Title")) (p ($text "hello world")))
  ;; <div>
  ;;   <h1>Title</h1>
  ;;   <p>hello world</p>
  ;; </div>

  (let [[x y & z] (div (h1 ($text "Hello")) (p ($text "ClojureScript")))]
    (assoc x :id "master"))

  ;; (h1 {:id "master"} ($text "Hello"))
  ;; <h1 id="master">Hello</h1>

  (reduce
    conj
    (div {:id "main"}
      (div
        (p ($text "whoa"))))
    (map #(assoc % :foo (gensym "thing"))
         (foo (p ($text "hello world")))))

  ;; (div {:id "main"} (h1 ($text "Title")) (p ($text "hello world")))
  ;; <div id="main">
  ;;   <div>
  ;;     <p>whoa</p>
  ;;   </div>
  ;;   <h1 foo="thing7">Title</h1>
  ;;   <p foo="thing8">hello world</p>
  ;; </div>

  )

(declare make-elem-node make-text-node)

(defprotocol IDomNode
  (-pr-node [n] "Get string representation of node.")
  (-tag [n] "Get node's tag string.")
  (-attrs [n] "Get node's attribute map.")
  (-dom [n] "Produce DOM tree for this node (and children)."))

(defn pr-node [n] (-pr-node n))
(defn tag [n] (-tag n))
(defn attrs [n] (-attrs n))
(defn dom [n] (-dom n))

(deftype TextNode [tag text]
  Object
  (toString [n] (pr-node n))

  IPrintable
  (-pr-seq [n opts]
    (js/console.log (dom n))
    (pr-node n))

  IDomNode
  (-pr-node [n]
    (str "(" (.-tag n) " " (pr-str (.-text n)) ")"))
  (-tag [n] (.-tag n))
  (-attrs [n] nil)
  (-dom [n]
    (jq/$ (if (= "$text" (.-tag n))
            (-> js/document (.createTextNode (.-text n)))
            (-> js/document (.createComment (.-text n)))))))

(defn make-text-node [text]
  (TextNode. "$text" text))

(defn make-comment-node [text]
  (TextNode. "$comment" text))

(deftype ElemNode [tag attrs children ids]
  Object
  (toString [n] (pr-node n))

  IFn
  (-invoke [n & args]
    (let [ntag      (.-tag n)
          nattrs    (.-attrs n)
          nchildren (.-children n)
          nids      (.-ids n)]
      (if (seq args)
        (let [[head & tail] args
              typ (type head)]
          (if (or (= typ ElemNode) (= typ TextNode))
            (make-elem-node ntag nattrs (into nchildren (vec args)) nids)
            (make-elem-node ntag (into nattrs head) (into nchildren (vec tail)) nids)))
        n)))

  IStack
  (-peek [n] (peek (.-children n)))
  (-pop [n] (make-elem-node (.-tag n) (.-attrs n) (pop (.-children n)) (.-ids n)))
  
  ICounted
  (-count [n] (count (.-children n)))

  IEmptyableCollection
  (-empty [n]
    (make-elem-node (.-tag n) (.-attrs n) [] (.-ids n)))

  ICollection
  (-conj [n o]
    (make-elem-node (.-tag n) (.-attrs n) (conj (.-children n) o) (.-ids n)))
 
  IIndexed
  (-nth [n i]
    (nth (.-children n) i)) 
  (-nth [n i not-found]
    (nth (.-children n) i not-found))

  ISeq
  (-first [n]
    (first (.-children n)))
  (-rest [n]
    (make-elem-node (.-tag n) (.-attrs n) (vec (rest (.-children n))) (.-ids n)))

  INext
  (-next [n]
    (let [nx (vec (next (.-children n)))]
      (if (seq nx)
        (make-elem-node (.-tag n) (.-attrs n) nx (.-ids n))
        nil)))

  ILookup
  (-lookup [n k]
    (k (.-attrs n)))
  (-lookup [n k not-found]
    (k (.-attrs n) not-found))

  IAssociative
  (-contains-key? [n k]
    (contains? (.-attrs n) k))
  (-assoc [n k v]
    (if (integer? k)
      (make-elem-node (.-tag n)
                      (.-attrs n)
                      (assoc (.-children n) k v)
                      (.-ids n))
      (make-elem-node (.-tag n) (assoc (.-attrs n) k v) (.-children n) (.-ids n))))

  IMap
  (-dissoc [n k]
    (assert (not (integer? k)) "Can't dissoc children")
    (make-elem-node (.-tag n) (dissoc (.-attrs n) k) (.-children n) (.-ids n)))

  ISeqable
  (-seq [n]
    (if (seq (.-children n)) n nil))

  IReversible
  (-rseq [n]
    (make-elem-node (.-tag n) (.-attrs n) (vec (reverse (.-children n))) (.-ids n)))

  IPrintable
  (-pr-seq [n opts]
    (js/console.log (dom n))
    (pr-node n))
  
  IDomNode
  (-pr-node [n]
    (let [tag       (.-tag n)
          attrs     (.-attrs n) 
          children  (.-children n)
          o-paren   (if (seq children) "(" "")
          c-paren   (if (seq children) ")" "")
          attrs-str (if (< 0 (count attrs)) (pr-str attrs) "")
          child-str (if (seq children) (join " " (map pr-node children)) "")
          str-parts (filter #(not= "" %) (list tag attrs-str child-str))
          str-all   (if (< 1 (count str-parts))
                      (concat '("(") str-parts '(")"))
                      str-parts)]
      (join " " str-all)))
  (-tag [n] (.-tag n))
  (-attrs [n] (.-attrs n))
  (-dom [n]
    (let [$elem       (jq/$ (-> js/document (.createElement (.-tag n)))) 
          ids         (.-ids n)
          attrs-noid  (.-attrs n)
          attrs       (if (seq ids)
                        (assoc attrs-noid :data-hl (join " " ids))
                        attrs-noid) 
          children    (mapv dom (.-children n))]
      (-> $elem
        (jq/attr attrs)
        (jq/append children)))))


(defn make-elem-node
  ([tag]
   (ElemNode. tag {} [] []))
  ([tag attrs]
   (ElemNode. tag attrs [] []))
  ([tag attrs kids]
   (ElemNode. tag attrs kids []))
  ([tag attrs kids ids]
   (ElemNode. tag attrs kids ids)))

(defn clone [n]
  (make-elem-node (.-tag n) (.-attrs n) (.-children n) (conj (.-ids n) (str (gensym)))))

(def a              (make-elem-node "a"))
(def abbr           (make-elem-node "abbr"))
(def acronym        (make-elem-node "acronym"))
(def address        (make-elem-node "address"))
(def applet         (make-elem-node "applet"))
(def area           (make-elem-node "area"))
(def article        (make-elem-node "article"))
(def aside          (make-elem-node "aside"))
(def audio          (make-elem-node "audio"))
(def b              (make-elem-node "b"))
(def base           (make-elem-node "base"))
(def basefont       (make-elem-node "basefont"))
(def bdi            (make-elem-node "bdi"))
(def bdo            (make-elem-node "bdo"))
(def big            (make-elem-node "big"))
(def blockquote     (make-elem-node "blockquote"))
(def body           (make-elem-node "body"))
(def br             (make-elem-node "br"))
(def button         (make-elem-node "button"))
(def canvas         (make-elem-node "canvas"))
(def caption        (make-elem-node "caption"))
(def center         (make-elem-node "center"))
(def cite           (make-elem-node "cite"))
(def code           (make-elem-node "code"))
(def col            (make-elem-node "col"))
(def colgroup       (make-elem-node "colgroup"))
(def command        (make-elem-node "command"))
(def data           (make-elem-node "data"))
(def datalist       (make-elem-node "datalist"))
(def dd             (make-elem-node "dd"))
(def del            (make-elem-node "del"))
(def details        (make-elem-node "details"))
(def dfn            (make-elem-node "dfn"))
(def dir            (make-elem-node "dir"))
(def div            (make-elem-node "div"))
(def dl             (make-elem-node "dl"))
(def dt             (make-elem-node "dt"))
(def em             (make-elem-node "em"))
(def embed          (make-elem-node "embed"))
(def eventsource    (make-elem-node "eventsource"))
(def fieldset       (make-elem-node "fieldset"))
(def figcaption     (make-elem-node "figcaption"))
(def figure         (make-elem-node "figure"))
(def font           (make-elem-node "font"))
(def footer         (make-elem-node "footer"))
(def form           (make-elem-node "form"))
(def frame          (make-elem-node "frame"))
(def frameset       (make-elem-node "frameset"))
(def h1             (make-elem-node "h1"))
(def h2             (make-elem-node "h2"))
(def h3             (make-elem-node "h3"))
(def h4             (make-elem-node "h4"))
(def h5             (make-elem-node "h5"))
(def h6             (make-elem-node "h6"))
(def head           (make-elem-node "head"))
(def header         (make-elem-node "header"))
(def hgroup         (make-elem-node "hgroup"))
(def hr             (make-elem-node "hr"))
(def html           (make-elem-node "html"))
(def i              (make-elem-node "i"))
(def iframe         (make-elem-node "iframe"))
(def img            (make-elem-node "img"))
(def input          (make-elem-node "input"))
(def ins            (make-elem-node "ins"))
(def isindex        (make-elem-node "isindex"))
(def kbd            (make-elem-node "kbd"))
(def keygen         (make-elem-node "keygen"))
(def label          (make-elem-node "label"))
(def legend         (make-elem-node "legend"))
(def li             (make-elem-node "li"))
(def link           (make-elem-node "link"))
(def html-map       (make-elem-node "map"))
(def mark           (make-elem-node "mark"))
(def menu           (make-elem-node "menu"))
(def html-meta      (make-elem-node "meta"))
(def meter          (make-elem-node "meter"))
(def nav            (make-elem-node "nav"))
(def noframes       (make-elem-node "noframes"))
(def noscript       (make-elem-node "noscript"))
(def object         (make-elem-node "object"))
(def ol             (make-elem-node "ol"))
(def optgroup       (make-elem-node "optgroup"))
(def option         (make-elem-node "option"))
(def output         (make-elem-node "output"))
(def p              (make-elem-node "p"))
(def param          (make-elem-node "param"))
(def pre            (make-elem-node "pre"))
(def progress       (make-elem-node "progress"))
(def q              (make-elem-node "q"))
(def rp             (make-elem-node "rp"))
(def rt             (make-elem-node "rt"))
(def ruby           (make-elem-node "ruby"))
(def s              (make-elem-node "s"))
(def samp           (make-elem-node "samp"))
(def script         (make-elem-node "script"))
(def section        (make-elem-node "section"))
(def select         (make-elem-node "select"))
(def small          (make-elem-node "small"))
(def source         (make-elem-node "source"))
(def span           (make-elem-node "span"))
(def strike         (make-elem-node "strike"))
(def strong         (make-elem-node "strong"))
(def style          (make-elem-node "style"))
(def sub            (make-elem-node "sub"))
(def summary        (make-elem-node "summary"))
(def sup            (make-elem-node "sup"))
(def table          (make-elem-node "table"))
(def tbody          (make-elem-node "tbody"))
(def td             (make-elem-node "td"))
(def textarea       (make-elem-node "textarea"))
(def tfoot          (make-elem-node "tfoot"))
(def th             (make-elem-node "th"))
(def thead          (make-elem-node "thead"))
(def html-time      (make-elem-node "time"))
(def title          (make-elem-node "title"))
(def tr             (make-elem-node "tr"))
(def track          (make-elem-node "track"))
(def tt             (make-elem-node "tt"))
(def u              (make-elem-node "u"))
(def ul             (make-elem-node "ul"))
(def html-var       (make-elem-node "var"))
(def video          (make-elem-node "video"))
(def wbr            (make-elem-node "wbr"))

(def $text          make-text-node)
(def $comment       make-comment-node)

(def *initfns* (atom []))

(defn add-initfn! [f]
  (swap! *initfns* into [f]))

(defn init [forms]
  (jq/$
    (fn []
      (let [$body (jq/$ "body")]
        (jq/empty $body)
        (mapv #(jq/append $body (dom %)) forms)
        (mapv (fn [f] (f)) @*initfns*)))))
