;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns hoplon.core
  (:require [goog.Uri]
            [goog.object          :as obj]
            [clojure.set          :refer [difference intersection]]
            [javelin.core         :refer [cell? cell lift destroy-cell!]]
            [cljs.reader          :refer [read-string]]
            [clojure.string       :refer [split join blank?]]
            [cljs.spec.alpha      :as spec]
            [cljs.spec.test.alpha :as spect]
            [hoplon.spec])
  (:require-macros [javelin.core :refer [with-let cell= prop-cell]]
                   [hoplon.core  :refer [with-timeout with-dom]]))

;; Console Printing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Declare Variables ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(declare elem! do! on! ->node $text add-children! attribute?)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Internal Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- child-vec
  [this]
  (let [x (.-childNodes this)]
    (areduce x i ret [] (conj ret (.item x i)))))

(defn- vflatten
  "Takes a sequential collection and returns a flattened vector of any nested
  sequential collections."
  ([x] (persistent! (vflatten (transient []) x)))
  ([acc x] (if (sequential? x) (reduce vflatten acc x) (conj! acc x))))

(defn- remove-nil [nodes]
  (reduce #(if %2 (conj %1 %2) %1) [] nodes))

(defn- compact-kids
  "Flattens nested sequencences of elements, removing nil values."
  [kids]
  (->>
    (vflatten kids)
    (remove-nil)
    (mapv ->node)))

(defn- set-dom-children!
  "Sets a DOM element's children to the sequence of children given."
  [elem new-kids]
  (let [new-kids (compact-kids new-kids)
        new?     (set new-kids)]
    (loop [[new-kid & nks]              new-kids
           [old-kid & oks :as old-kids] (child-vec elem)]
      (when (or new-kid old-kid)
        (cond
          (= new-kid old-kid) (recur nks oks)
          (not old-kid)       (do (.appendChild elem new-kid)
                                  (recur nks oks))
          (not new-kid)       (do (when-not (new? old-kid) (.removeChild elem old-kid))
                                  (recur nks oks))
          :else               (do (.insertBefore elem new-kid old-kid)
                                  (recur nks old-kids)))))))

(defn- -do! [elem this value]
  (do! elem this value))

(spec/fdef -do! :args :hoplon.spec/do! :ret any?)

(defn- -on! [elem this value]
  (on! elem this value))

(spec/fdef -on! :args :hoplon.spec/on! :ret any?)

(defn- -elem! [elem this value]
  (elem! elem this value))

(spec/fdef -elem! :args :hoplon.spec/elem! :ret any?)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Public Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def prerendering?
  "Is the application running in a prerendering container (eg. PhantomJS via
  the prerender task)?"
  (.getParameterValue (goog.Uri. (.. js/window -location -href)) "prerendering"))

(defn do-watch
  "Adds f as a watcher to ref and evaluates (f init @ref) once. The watcher
  f is a function of two arguments: the previous and next values. If init is
  not provided the default (nil) will be used."
  ([ref f]
   (do-watch ref nil f))
  ([ref init f]
   (with-let [k (gensym)]
     (f init @ref)
     (add-watch ref k (fn [_ _ old new] (f old new))))))

(defn normalize-class
  "Class normalization for attribute providers. Converts from strings and
  sequences to hashmaps."
  [kvs]
  (let [->map #(zipmap % (repeat true))]
    (if (map? kvs)
      kvs
      (->map (if (string? kvs) (.split kvs #"\s+") (seq kvs))))))

(defn timeout
  "Executes a fuction after a delay, if no delay is passed, 0 is used as a default."
  ([f] (timeout f 0))
  ([f t] (.setTimeout js/window f t)))

(defn when-dom
  "Executes a function once an element has been attached to the DOM."
  [this f]
  (if-not (instance? js/Element this)
    (with-timeout 0 (f))
    (if-let [v (obj/get this "_hoplonWhenDom")]
      (.push v f)
      (do (obj/set this "_hoplonWhenDom" (array f))
          (with-timeout 0
            ((fn doit []
               (if-not (.contains (.-documentElement js/document) this)
                 (with-timeout 20 (doit))
                 (do (doseq [f (obj/get this "_hoplonWhenDom")] (f))
                     (obj/set this "_hoplonWhenDom" nil))))))))))

(defn add-initfn!
  "Executes a function once the window load event is fired."
  [f] (.addEventListener js/window "load" #(with-timeout 0 (f))))

(defn parse-args
  "Parses a sequence of element arguments into attributes and children."
  [args]
  (loop [attr (transient {})
         kids (transient [])
         [arg & args] args]
    (if-not (or arg args)
      [(persistent! attr) (persistent! kids)]
      (cond (map? arg)       (recur (reduce-kv assoc! attr arg) kids args)
            (set? arg)       (recur (reduce #(assoc! %1 %2 true) attr arg) kids args)
            (attribute? arg) (recur (assoc! attr arg (first args)) kids (rest args))
            (seq? arg)       (recur attr (reduce conj! kids (vflatten arg)) args)
            (vector? arg)    (recur attr (reduce conj! kids (vflatten arg)) args)
            :else            (recur attr (conj! kids arg) args)))))

(defn kw-dispatcher
  "A multi-method dispatch function.

   Will dispatch against three arguments:

     * `elem`  - the target DOM Element containing the attribute
     * `key`   - the attribute keyword
     * `value` - the attribute value

   The kw-dispatcher will attempt to dispatch agains the key argument.

   ex. when key is `:namespace/key` will dispatch on `:namespace/key`"
  [elem key value] key)

(defn ns-dispatcher
  "A multi-method dispatch function.

   Will dispatch against three arguments:

     * `elem`  - the target DOM Element containing the attribute
     * `key`   - the attribute keyword
     * `value` - the attribute value

   The ns-dispatcher will attempt to dispatch agains the key namespace or key.

   ex. when key is `:namespace/key` will dispatch on `:namespace/default` otherwise `:namespace/key`"
  [elem key value]
  (if-let [n (namespace key)]
    (keyword n "default") key))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon Nodes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IHoplonNode
  (-node [this]))

(defn node? [this]
  (satisfies? IHoplonNode this))

(extend-protocol IHoplonNode
  string
  (-node [this]
    ($text this))
  number
  (-node [this]
    ($text (str this))))

(defn- ->node
  [x]
  (if (node? x) (-node x) x))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IHoplonAttribute
  (-attribute! [this elem value]))

(defn attribute? [this]
  (satisfies? IHoplonAttribute this))

(extend-protocol IHoplonAttribute
  Keyword
  (-attribute! [this elem value]
    (-elem! elem this value)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon Runtime Spec ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn spec! []
  (spect/instrument `-elem!)
  (spect/instrument `-do!)
  (spect/instrument `-on!))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon Elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IHoplonElement
  (-set-attributes! [this kvs]
    "Sets attributes on a managed element using native functionality.")
  (-set-styles!     [this kvs]
    "Sets styles on a managed element using native functionality.")
  (-hoplon-kids     [this]
    "Returns the hoplon managed kids atom, or creates it if missing.")
  (-append-child!   [this child]
    "Appends `child` to `this` for the case of `this` being a managed element.")
  (-remove-child!   [this child]
    "Removes `child` from `this` for the case of `this` being a managed element.")
  (-replace-child!  [this new existing]
    "Replaces `existing` with `new` in `this` for the case of `this` being a managed element.")
  (-insert-before!  [this new existing]
    "Inserts `existing` before `new` in `this` for the case of `this` being a managed element."))

(defn element?
  "Returns true if elem is a managed element. Managed elements have
  their children managed by Hoplon and implement the IHoplonElement protocol."
  [this]
  (and
    (instance? js/Element this)
    (satisfies? IHoplonElement this)))

(defn native?
  "Returns true if elem is a native element. Native elements' children
  are not managed by Hoplon, and have not been extended with IHoplonElement."
  [elem]
  (and
    (instance? js/Element elem)
    (not (element? elem))))

(defn native-node?
 [node]
 "Returns true if node is any native node. Same as native? but allows for nodes
 that are not elements."
 (and
  (instance? js/Node node)
  (not (element? node))))

(defn hoplonify! [elem]
  (specify! elem
    IPrintWithWriter
    (-pr-writer
      ([this writer opts]
       (write-all writer "#<HoplonElement: " (.-tagName this) ">")))
    ILookup
    (-lookup
      ([this k]
       (if (attribute? k)
         (.getAttribute this (name k))
         (obj/get (.-children this) k)))
      ([this k not-found]
       (or (-lookup this k) not-found)))
    IHoplonElement
    (-set-attributes!
      ([this kvs]
       (let [e this]
         (doseq [[k v] kvs :let [k (name k)]]
           (if-not v
             (.removeAttribute e k)
             (.setAttribute e k (if (true? v) k v)))))))
    (-set-styles!
      ([this kvs]
       (let [e this]
         (doseq [[k v] kvs]
           (obj/set (.. e -style) (name k) (str v))))))
    (-hoplon-kids
      ([this]
       (if-let [hl-kids (.-hoplonKids this)] hl-kids
         (with-let [kids (atom (child-vec this))]
           (set! (.-hoplonKids this) kids)
           (do-watch kids #(set-dom-children! this %2))))))
    (-append-child!
      ([this child]
       (with-let [child child]
         (let [kids (-hoplon-kids this)
               i    (count @kids)]
           (if (cell? child)
             (do-watch child #(swap! kids assoc i %2))
             (swap! kids assoc i child))))))
    (-remove-child!
      ([this child]
       (with-let [child child]
        (let [kids (-hoplon-kids this)
              before-count (count @kids)]
         (if (cell? child)
           (swap! kids #(vec (remove (partial = @child) %)))
           (swap! kids #(vec (remove (partial = child) %))))
         (when-not (= (count @kids) (dec before-count))
          (throw (js/Error. "Attempted to remove a node that is not a child of parent.")))))))
    (-replace-child!
      ([this new existing]
       (with-let [existing existing]
        (swap! (-hoplon-kids this) #(mapv (fn [el] (if (= el existing) new el)) %)))))
    (-insert-before!
      ([this new existing]
       (with-let [new new]
        (cond
         (not existing) (swap! (-hoplon-kids this) conj new)
         (not= new existing) (swap! (-hoplon-kids this) #(vec (mapcat (fn [el] (if (= el existing) [new el] [el])) %)))))))))

(defn ->hoplon [elem]
  (if (element? elem) elem
    (with-let [_ elem]
      (hoplonify! elem))))

(defn set-attributes!
  ([this kvs]
   (-set-attributes! (->hoplon this) kvs))
  ([this k v & kvs]
   (set-attributes! this (apply hash-map k v kvs))))

(defn set-styles!
  ([this kvs]
   (-set-styles! (->hoplon this) kvs))
  ([this k v & kvs]
   (set-styles! this (apply hash-map k v kvs))))

(defn append-child!
  [this child]
  (-append-child! (->hoplon this) child))

(defn remove-child!
  [this child]
  (-remove-child! (->hoplon this) child))

(defn replace-child!
  [this new existing]
  (-replace-child! (->hoplon this) new existing))

(defn insert-before!
  [this new existing]
  (-insert-before! (->hoplon this) new existing))

(defn add-attributes!
  [this attr]
  (-elem! this :hoplon/attr attr))

(defn add-children!
  [this kids]
  (-elem! this :hoplon/kids kids))

(defn invoke!
  [this & args]
  (-elem! this :hoplon/invoke args))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon elem! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti elem! ns-dispatcher :default ::default)

(defmethod elem! ::default
  [elem key value]
  (cond (cell? value) (do-watch value #(-do! elem key %2))
        (fn? value)   (-on! elem key value)
        :else         (-do! elem key value)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon hl! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti hl! kw-dispatcher)

(defmethod elem! :hoplon/default
  [elem key args]
  (hl! elem key args))

(defmethod hl! :hoplon/singleton
  [elem key args]
  (let [[attr kids] (parse-args args)]
    (if (:hoplon/static attr) elem
      (doto (->hoplon elem)
        (hl! :hoplon/reset nil)
        (hl! :hoplon/attr attr)
        (hl! :hoplon/kids kids)))))

(defmethod hl! :hoplon/reset
  [elem key val]
  (with-let [elem elem]
    (let [kids (-hoplon-kids elem)]
      (swap! kids empty)
      (doseq [w (keys (.-watches kids))]
        (remove-watch kids w))
      (set! (.-hoplonKids elem) val))))

(defmethod hl! :hoplon/invoke
  [elem key args]
  (let [[attr kids] (parse-args args)]
    (if (:hoplon/static attr) elem
      (doto (->hoplon elem)
        (hl! :hoplon/attr attr)
        (hl! :hoplon/kids kids)))))

(defmethod hl! :hoplon/attr
  [elem key attr]
  (with-let [elem elem]
    (reduce-kv #(do (-attribute! %2 %1 %3) %1) elem attr)))

(defmethod hl! :hoplon/kids
  [elem key kids]
  (with-let [elem elem]
    (doseq [x (vflatten kids)]
      (when-let [x (->node x)]
        (-append-child! elem x)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon do! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti do! ns-dispatcher :default ::default)

(defmethod do! ::default
  [elem k v]
  (set-attributes! elem k v))

(defmethod do! ::attr
  [elem key kvs]
  (set-attributes! elem kvs))

(derive :attr         ::attr)
(derive :attr/default ::attr)
(derive :html/default ::attr)
(derive :svg/default  ::attr)

(defmethod do! ::css
  [elem _ kvs]
  (set-styles! elem kvs))

(derive :css         ::css)
(derive :css/default ::css)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon on! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti on! ns-dispatcher :default ::default)

(defmethod on! ::default
  [elem event callback]
  (.addEventListener elem (name event) callback))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; HTML Elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(extend-type js/Element
  IPrintWithWriter
  (-pr-writer
    ([this writer opts]
     (write-all writer "#<Element: " (.-tagName this) ">")))
  IFn
  (-invoke
    ([this]
     (invoke! this))
    ([this a]
     (invoke! this a))
    ([this a b]
     (invoke! this a b))
    ([this a b c]
     (invoke! this a b c))
    ([this a b c d]
     (invoke! this a b c d))
    ([this a b c d e]
     (invoke! this a b c d e))
    ([this a b c d e f]
     (invoke! this a b c d e f))
    ([this a b c d e f g]
     (invoke! this a b c d e f g))
    ([this a b c d e f g h]
     (invoke! this a b c d e f g h))
    ([this a b c d e f g h i]
     (invoke! this a b c d e f g h i))
    ([this a b c d e f g h i j]
     (invoke! this a b c d e f g h i j))
    ([this a b c d e f g h i j k]
     (invoke! this a b c d e f g h i j k))
    ([this a b c d e f g h i j k l]
     (invoke! this a b c d e f g h i j k l))
    ([this a b c d e f g h i j k l m]
     (invoke! this a b c d e f g h i j k l m))
    ([this a b c d e f g h i j k l m n]
     (invoke! this a b c d e f g h i j k l m n))
    ([this a b c d e f g h i j k l m n o]
     (invoke! this a b c d e f g h i j k l m n o))
    ([this a b c d e f g h i j k l m n o p]
     (invoke! this a b c d e f g h i j k l m n o p))
    ([this a b c d e f g h i j k l m n o p q]
     (invoke! this a b c d e f g h i j k l m n o p q))
    ([this a b c d e f g h i j k l m n o p q r]
     (invoke! this a b c d e f g h i j k l m n o p q r))
    ([this a b c d e f g h i j k l m n o p q r s]
     (invoke! this a b c d e f g h i j k l m n o p q r s))
    ([this a b c d e f g h i j k l m n o p q r s t]
     (invoke! this a b c d e f g h i j k l m n o p q r s t))
    ([this a b c d e f g h i j k l m n o p q r s t rest]
     (invoke! this a b c d e f g h i j k l m n o p q r s t rest))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; HTML Constructors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- mksingleton [tag]
  "Retrieves the DOM element `elem` from js/document and updates in-place.

  Creates the element if missing."
  (fn [& args]
    (if-let [elem (obj/get js/document tag)]
      (-elem! elem :hoplon/singleton args)
      (with-let [elem (.createElement js/document tag)]
        (obj/set js/document tag elem)
        (-elem! elem :hoplon/invoke args)))))

(defn- mkelem [tag]
  "Returns a DOM element function.

  This creates a DOM element of type `tag` and invokes it."
  (fn [& args]
    (with-let [elem (.createElement js/document tag)]
      (-elem! elem :hoplon/invoke args))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; HTML Elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn html [& args]
 "Updates and returns the document's `html` element in place."
  (let [elem (mksingleton "documentElement")
        [attr kids] (parse-args args)]
   (elem kids)))

(def head
 "Updates and returns the document's `head` element in place."
 (mksingleton "head"))

(def body
 "Updates and returns the document's `body` element in place."
 (mksingleton "body"))

(def a              (mkelem "a"))
(def abbr           (mkelem "abbr"))
(def address        (mkelem "address"))
(def area           (mkelem "area"))
(def article        (mkelem "article"))
(def aside          (mkelem "aside"))
(def audio          (mkelem "audio"))
(def b              (mkelem "b"))
(def base           (mkelem "base"))
(def bdi            (mkelem "bdi"))
(def bdo            (mkelem "bdo"))
(def blockquote     (mkelem "blockquote"))
(def br             (mkelem "br"))
(def button         (mkelem "button"))
(def canvas         (mkelem "canvas"))
(def caption        (mkelem "caption"))
(def cite           (mkelem "cite"))
(def code           (mkelem "code"))
(def col            (mkelem "col"))
(def colgroup       (mkelem "colgroup"))
(def data           (mkelem "data"))
(def datalist       (mkelem "datalist"))
(def dd             (mkelem "dd"))
(def del            (mkelem "del"))
(def details        (mkelem "details"))
(def dfn            (mkelem "dfn"))
(def dialog         (mkelem "dialog"))
(def div            (mkelem "div"))
(def dl             (mkelem "dl"))
(def dt             (mkelem "dt"))
(def em             (mkelem "em"))
(def embed          (mkelem "embed"))
(def fieldset       (mkelem "fieldset"))
(def figcaption     (mkelem "figcaption"))
(def figure         (mkelem "figure"))
(def footer         (mkelem "footer"))
(def form           (mkelem "form"))
(def h1             (mkelem "h1"))
(def h2             (mkelem "h2"))
(def h3             (mkelem "h3"))
(def h4             (mkelem "h4"))
(def h5             (mkelem "h5"))
(def h6             (mkelem "h6"))
(def header         (mkelem "header"))
(def hgroup         (mkelem "hgroup"))
(def hr             (mkelem "hr"))
(def i              (mkelem "i"))
(def iframe         (mkelem "iframe"))
(def img            (mkelem "img"))
(def input          (mkelem "input"))
(def ins            (mkelem "ins"))
(def kbd            (mkelem "kbd"))
(def keygen         (mkelem "keygen"))
(def label          (mkelem "label"))
(def legend         (mkelem "legend"))
(def li             (mkelem "li"))
(def link           (mkelem "link"))
(def main           (mkelem "main"))
(def html-map       (mkelem "map"))
(def mark           (mkelem "mark"))
(def menu           (mkelem "menu"))
(def menuitem       (mkelem "menuitem"))
(def html-meta      (mkelem "meta"))
(def meter          (mkelem "meter"))
(def multicol       (mkelem "multicol"))
(def nav            (mkelem "nav"))
(def noframes       (mkelem "noframes"))
(def noscript       (mkelem "noscript"))
(def html-object    (mkelem "object"))
(def ol             (mkelem "ol"))
(def optgroup       (mkelem "optgroup"))
(def option         (mkelem "option"))
(def output         (mkelem "output"))
(def p              (mkelem "p"))
(def param          (mkelem "param"))
(def picture        (mkelem "picture"))
(def pre            (mkelem "pre"))
(def progress       (mkelem "progress"))
(def q              (mkelem "q"))
(def rp             (mkelem "rp"))
(def rt             (mkelem "rt"))
(def rtc            (mkelem "rtc"))
(def ruby           (mkelem "ruby"))
(def s              (mkelem "s"))
(def samp           (mkelem "samp"))
(def script         (mkelem "script"))
(def section        (mkelem "section"))
(def select         (mkelem "select"))
(def shadow         (mkelem "shadow"))
(def small          (mkelem "small"))
(def source         (mkelem "source"))
(def span           (mkelem "span"))
(def strong         (mkelem "strong"))
(def style          (mkelem "style"))
(def sub            (mkelem "sub"))
(def summary        (mkelem "summary"))
(def sup            (mkelem "sup"))
(def table          (mkelem "table"))
(def tbody          (mkelem "tbody"))
(def td             (mkelem "td"))
(def template       (mkelem "template"))
(def textarea       (mkelem "textarea"))
(def tfoot          (mkelem "tfoot"))
(def th             (mkelem "th"))
(def thead          (mkelem "thead"))
(def html-time      (mkelem "time"))
(def title          (mkelem "title"))
(def tr             (mkelem "tr"))
(def track          (mkelem "track"))
(def u              (mkelem "u"))
(def ul             (mkelem "ul"))
(def html-var       (mkelem "var"))
(def video          (mkelem "video"))
(def wbr            (mkelem "wbr"))

(def spliced        vector)
(def $text          #(.createTextNode js/document %))
(def $comment       #(.createComment js/document %))

(def <!--           $comment)
(def -->            ::-->)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Template Macro Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn loop-tpl*
  "Given a cell items containing a seqable collection, constructs a cell that
  works like a fill vector. The template `tpl` is a function of one argument: the
  formula cell containing the ith item in items. The tpl function is called
  once (and only once) for each index in items. When the items collection
  shrinks the DOM element created by the template is not destroyed--it is only
  removed from the DOM and cached. When the items collection grows again those
  cached elements will be reinserted into the DOM at their original index."
  [items tpl]
  (let [els         (cell [])
        itemsv      (cell= (vec items))
        items-count (cell= (count items))]
    (do-watch items-count
              (fn [_ n]
                (when (< (count @els) n)
                  (doseq [i (range (count @els) n)]
                    (swap! els assoc i (tpl (cell= (get itemsv i nil))))))))
    (cell= (subvec els 0 (min items-count (count els))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
