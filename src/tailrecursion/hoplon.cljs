(ns tailrecursion.hoplon
  (:require-macros
    [tailrecursion.javelin :refer [cell=]])
  (:require
    tailrecursion.javelin
    [goog.dom         :as gdom]
    [clojure.zip      :as zip]
    [clojure.string   :refer [join blank?]]))

(defn safe-name [x]
  (try (name x) (catch js/Error e)))

(defn safe-nth
  ([coll index] (safe-nth coll index nil))
  ([coll index not-found]
   (try (nth coll index not-found) (catch js/Error _ not-found))))

;; env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare make-elem-node make-text-node)

(defn append-child [elem children]
  (when (and elem children) 
    (gdom/appendChild elem children)))

(defprotocol IDomNode
  (-pr-node [n] "Get string representation of node.")
  (-tag [n] "Get node's tag string.")
  (-attrs [n] "Get node's attribute map.")
  (-branch? [n] "True if this node is a branch.")
  (-children [n] "Returns a seq of child nodes.")
  (-make-node [n children] "Given a node and a seq of children, make a new node.")
  (-dom [n] "Produce DOM tree for this node (and children)."))

(defn pr-node [n] (-pr-node n))

(defn tag [n] (-tag n))

(defn attrs [n] (-attrs n))

(defn branch? [n] (-branch? n))

(defn children [n] (-children n))

(defn make-node [n children] (-make-node n children))

(defn dom [n] (if (satisfies? IDomNode n) (-dom n) nil))

(defn node-zip [root]
  (zip/zipper branch? children make-node root))

(deftype TextNode [tag text mymeta]
  Object
  (toString [n] (.-text n))

  IPrintWithWriter
  (-pr-writer [n writer _]
    (js/console.log (dom n))
    (-write writer (pr-node n)))

  IMeta
  (-meta [n] (.-mymeta n))

  IWithMeta
  (-with-meta [n new-meta]
    (TextNode. (.-tag n) (.-text n) new-meta))

  IDomNode
  (-pr-node [n]
    (str "(" (.-tag n) " " (pr-str (.-text n)) ")"))
  (-tag [n] (.-tag n))
  (-attrs [n] nil)
  (-branch? [n] false)
  (-children [n] (assert false "Text nodes can't have children."))
  (-make-node [n kids] (make-text-node (.-tag n) (.-text n)))
  (-dom [n]
    (let [node (if (= "$text" (.-tag n))
                 (-> js/document (.createTextNode (.-text n)))
                 (-> js/document (.createComment (.-text n))))
          hlr  (or (::on-create (meta n)) identity)]
      (doto node hlr))))

(defn make-text-node [text]
  (TextNode. "$text" text nil))

(defn make-comment-node [text]
  (TextNode. "$comment" text nil))

(deftype Spliced [tag children]
  IFn
  (-invoke [n & args]
    (Spliced. (.-tag n) (into (.-children n) (vec args)))))

(defn splice [forms]
  (mapcat #(if (instance? Spliced %) (splice (.-children %)) [%]) forms))

(deftype ElemNode [tag attrs children ids mymeta]
  Object
  (toString [n] (pr-node n))

  IFn
  (-invoke [n & args]
    (let [ntag      (.-tag n)
          nattrs    (.-attrs n)
          nchildren (.-children n)
          nids      (.-ids n)
          nmeta     (.-mymeta n)
          nargs     (map #(if (string? %) (make-text-node %) %) (splice args))
          cleanup   (fn [nodes] (vec (filter #(satisfies? IDomNode %) nodes)))]
      (if (seq nargs)
        (let [[head & tail] nargs]
          (if (satisfies? IDomNode head)
            (make-elem-node ntag
                            nattrs
                            (into nchildren (cleanup nargs))
                            nids
                            nmeta)
            (make-elem-node ntag
                            (into nattrs head)
                            (into nchildren (cleanup tail))
                            nids
                            nmeta)))
        n)))

  IMeta
  (-meta [n] (.-mymeta n))

  IWithMeta
  (-with-meta [n new-meta]
    (make-elem-node (.-tag n)
                    (.-attrs n)
                    (.-children n)
                    (.-ids n)
                    new-meta))

  IStack
  (-peek [n] (peek (.-children n)))
  (-pop [n] (make-elem-node (.-tag n)
                            (.-attrs n)
                            (pop (.-children n))
                            (.-ids n)
                            (.-mymeta n)))
  
  ICounted
  (-count [n] (count (.-children n)))

  IEmptyableCollection
  (-empty [n]
    (make-elem-node (.-tag n)
                    (.-attrs n)
                    []
                    (.-ids n)
                    (.-mymeta n)))

  ICollection
  (-conj [n o]
    (make-elem-node (.-tag n)
                    (.-attrs n)
                    (conj (.-children n) o)
                    (.-ids n)
                    (.-mymeta n)))
 
  IIndexed
  (-nth [n i]
    (nth (.-children n) i)) 
  (-nth [n i not-found]
    (nth (.-children n) i not-found))

  ISeq
  (-first [n]
    (first (.-children n)))
  (-rest [n]
    (make-elem-node (.-tag n)
                    (.-attrs n)
                    (vec (rest (.-children n)))
                    (.-ids n)
                    (.-mymeta n)))

  INext
  (-next [n]
    (let [nx (vec (next (.-children n)))]
      (if (seq nx)
        (make-elem-node (.-tag n)
                        (.-attrs n)
                        nx
                        (.-ids n)
                        (.-mymeta n))
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
                      (.-ids n)
                      (.-mymeta n))
      (make-elem-node (.-tag n)
                      (assoc (.-attrs n) k v)
                      (.-children n)
                      (.-ids n)
                      (.-mymeta n))))

  IMap
  (-dissoc [n k]
    (assert (not (integer? k)) "Can't dissoc children")
    (make-elem-node (.-tag n)
                    (dissoc (.-attrs n) k)
                    (.-children n)
                    (.-ids n)
                    (.-mymeta n)))

  ISeqable
  (-seq [n]
    (if (seq (.-children n)) n nil))

  IReversible
  (-rseq [n]
    (make-elem-node (.-tag n)
                    (.-attrs n)
                    (vec (reverse (.-children n)))
                    (.-ids n)
                    (.-mymeta n)))

  IPrintWithWriter
  (-pr-writer [n writer _]
    (js/console.log (dom n))
    (-write writer (pr-node n)))
  
  IDomNode
  (-pr-node [n]
    (let [tag         (.-tag n)
          attrs       (.-attrs n) 
          children    (.-children n)
          need-paren? (or (seq attrs) (seq children))
          o-paren     (if need-paren? "(" "")
          c-paren     (if need-paren? ")" "")
          attrs-str   (if (< 0 (count attrs)) (pr-str attrs) "")
          child-str   (if (seq children) (join " " (map pr-node children)) "")
          body        (join " " (remove blank? [tag attrs-str child-str]))]
      (str o-paren body c-paren)))
  (-tag [n] (.-tag n))
  (-attrs [n] (.-attrs n))
  (-branch? [n] true)
  (-children [n] (seq (.-children n)))
  (-make-node [n kids]
    (make-elem-node (.-tag n) (.-attrs n) (vec kids) (.-ids n) (.-mymeta n)))
  (-dom [n]
    (let [elem        (.createElement js/document (.-tag n)) 
          hlr         (or (::on-create (meta n)) identity)
          ids         (.-ids n)
          attrs-noid  (.-attrs n)
          attrs       (if (seq ids)
                        (assoc attrs-noid :data-hl (join " " ids))
                        attrs-noid)
          children    (mapv dom (.-children n))]
      (gdom/setProperties elem (clj->js attrs))
      (mapv #(.setAttribute elem (name (first %)) (str (second %))) attrs)
      (mapv #(append-child elem %) children)
      (doto elem hlr))))

(defn make-elem-node
  ([tag]
   (ElemNode. tag {} [] [] nil))
  ([tag attrs]
   (ElemNode. tag attrs [] [] nil))
  ([tag attrs kids]
   (ElemNode. tag attrs kids [] nil))
  ([tag attrs kids ids]
   (ElemNode. tag attrs kids ids nil))
  ([tag attrs kids ids mymeta]
   (ElemNode. tag attrs kids ids mymeta)))

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

(def spliced        (Spliced. "spliced" []))

(def $text          make-text-node)
(def $comment       make-comment-node)

(defn text [txt] txt)

(def *initfns* (atom []))

(defn add-initfn! [f]
  (swap! *initfns* into [f]))

(defn init [forms]
  (let [body  (.-body js/document)]
    (gdom/removeChildren body)
    (mapv #(append-child body (dom %)) (splice forms))
    (mapv (fn [f] (f)) @*initfns*)))

;; frp ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare ids)

(defn timeout [f t] (.setTimeout js/window f t))

(defn id      [e] (peek (ids e)))
(defn ids     [e] (.-ids e))
(defn id!     [e] (if-not (seq (ids e)) (clone e) e))
(defn is-jq?  [e] (string? (.-jquery e)))

(->
  (js/jQuery "body")
  (.on "submit" (fn [event] (.preventDefault event))))

(defn filter-id
  [x]
  (fn [v]
    (< 0 (->
           (js/jQuery (.-target v))
           (.parentsUntil "body")
           (.andSelf)
           (.filter (str "[data-hl~='" x "']"))
           (.size)))))

(defn find-id
  [x]
  (js/jQuery (str "[data-hl~='" x "']")))

(defn dom-get
  [elem]
  (if (satisfies? IDomNode elem)
    (find-id (id elem))
    (js/jQuery elem)))

(defn rel
  [other event]
  (let [os (js->clj (.offset (dom-get other))) 
        ox (os "left")
        oy (os "top")]
    {:x (- (.-pageX event) ox) :y (- (.-pageY event) oy)}))

(defn relx
  [other event]
  (:x (rel other event)))

(defn rely
  [other event]
  (:y (rel other event)))

(defn rel-event
  [rel-to tag handler]
  (fn [event]
    (aset event (str tag "X") (relx rel-to event))
    (aset event (str tag "Y") (rely rel-to event))
    (handler event)))

(defn text-val!
  ([e]
   (.val e))
  ([e v]
   (-> e
     (.val (str v))
     (.trigger "change"))))

(defn check-val!
  ([e]
   (.is e ":checked"))
  ([e v]
   (-> e
     (.prop "checked" (boolean v))
     (.trigger "change"))))

(defmulti do! (fn [elem action & args] action))

(defmethod do! :value
  [elem _ & args] 
  (let [e (dom-get elem)]
    (case (.attr e "type")
      "checkbox" (apply check-val! e args)
      (apply text-val! e args))))

(defmethod do! :attr
  ([elem _ k]
   (.attr (dom-get elem) (name k)))
  ([elem _ k v & kvs]
   (js/jQuery
     #(let [e (dom-get elem)] 
        (mapv (fn [[k v]]
                (when-let [k (safe-name k)]
                  (case v
                    true   (.attr e k k)
                    false  (.removeAttr e k)
                    (.attr e k (str v)))))
              (partition 2 (list* k v kvs)))))))

(defmethod do! :class
  ([elem _ c] 
   (when-let [c (safe-name c)]
     (js/jQuery #(.toggleClass (dom-get elem) c)))) 
  ([elem _ c switch & cswitches] 
   (js/jQuery
     (fn []
       (mapv (partial apply #(when-let [c (safe-name %1)]
                               (.toggleClass (dom-get elem) c (boolean %2)))) 
             (partition 2 (list* c switch cswitches)))))))

(defmethod do! :css
  ([elem _ k]
   (js/jQuery #(.css (dom-get elem) (safe-name k))))
  ([elem _ k v]
   (js/jQuery #(.css (dom-get elem) (safe-name k) v)))
  ([elem _ k v & more]
   (js/jQuery #(mapv (fn [[k v]] (do! elem :css k v))
                     (cons (list k v) (partition 2 more))))))

(defmethod do! :toggle
  [elem _ v]
  (js/jQuery #(.toggle (dom-get elem) (boolean v))))

(defmethod do! :slide-toggle
  [elem _ v]
  (js/jQuery
    #(if v
       (.slideDown (.hide (dom-get elem)) "fast")
       (.slideUp (dom-get elem) "fast"))))

(defmethod do! :fade-toggle
  [elem _ v]
  (js/jQuery
    #(if v
       (.fadeIn (.hide (dom-get elem)) "fast")
       (.fadeOut (dom-get elem) "fast"))))

(defmethod do! :focus
  [elem _ v]
  (js/jQuery #(if v (timeout (fn [] (.focus (dom-get elem))) 0)
                    (timeout (fn [] (.focusout (dom-get elem))) 0))))

(defmethod do! :select
  [elem _ _]
  (js/jQuery #(.select (dom-get elem))))

(defmethod do! :focus-select
  [elem _ v]
  (js/jQuery #(when v (do! elem :focus v) (do! elem :select v))))

(defmethod do! :text
  [elem _ v]
  (js/jQuery #(.text (dom-get elem) (str v))))

(def events (atom {}))

(defn- delegate
  [atm event]
  (.on (js/jQuery js/document) event "[data-hl]" #(reset! atm %))
  atm)

(defn add-event! [& event-keys]
  (doseq [event event-keys]
    (swap! events assoc (keyword event) (delegate (atom nil) (name event)))))

(add-event! :change :click :dblclick :error :focus :focusin :focusout
            :hover :keydown :keypress :keyup :load :mousedown :mouseenter
            :mouseleave :mousemove :mouseout :mouseover :mouseup :ready
            :scroll :select :submit :unload)

(defn- do-on!
  [elem event callback]
  (let [event   (get @events (keyword event))
        update  #(if (and (not= %3 %4) ((filter-id (id elem)) %4)) (callback %4))]
    (add-watch event (gensym) update)))

(defn on!
  [elem & event-callbacks]
  (mapv (partial apply do-on! elem) (partition 2 event-callbacks)))

(defn thing-looper [things g]
  (fn [f container]
    (into container (mapv #(apply f % (g things % (cell= (safe-nth things %))))
                          (range 0 (count @things))))))

;;; Deprecated api---these functions are here for backward compatibility.
(defn make-deprecated [key] (fn [elem & args] (apply do! elem key args)))
(def class!         (make-deprecated :class))
(def attr!          (make-deprecated :attr))
(def value!         (make-deprecated :value))
(def css!           (make-deprecated :css))
(def toggle!        (make-deprecated :toggle))
(def slide-toggle!  (make-deprecated :slide-toggle))
(def fade-toggle!   (make-deprecated :fade-toggle))
(def focus!         (make-deprecated :focus))
(def select!        (make-deprecated :select))
(def text!          (make-deprecated :text))
