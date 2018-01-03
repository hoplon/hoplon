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
  (let [x (.-childNodes this)
        l (.-length x)]
    (loop [i 0 ret (transient [])]
      (or (and (= i l) (persistent! ret))
          (recur (inc i) (conj! ret (.item x i)))))))

(defn- vflatten
 ([tree]
  (persistent! (vflatten tree (transient []))))
 ([tree ret]
  (let [l (count tree)]
    (loop [i 0]
      (if (= i l) ret
        (let [x (nth tree i)]
          (if-not (sequential? x)
            (conj! ret x)
            (vflatten x ret))
          (recur (inc i))))))))

(defn- merge-kids
  [this _ new]
  (let [new  (->> (vflatten new) (reduce #(if (nil? %2) %1 (conj %1 %2)) []) (mapv ->node))
        new? (set new)]
    (loop [[x & xs] new
           [k & ks :as kids] (child-vec this)]
      (when (or x k)
        (recur xs
          (cond
            (= x k) ks
            (not k) (with-let [ks ks]
                      (.appendChild this x))
            (not x) (with-let [ks ks]
                      (when-not (new? k)
                        (.removeChild this k)))
            :else   (with-let [kids kids]
                      (.insertBefore this x k))))))))

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

(defn dispatcher
  "A multi-method dispatch function.

   Will dispatch against three arguments:

     * `elem` - the target DOM Element containing the attribute
     * `key` - the attribute keyword or symbol
     * `value` - the attribute value

   The dispatcher will attempt to dispatch agains the key namespace or key.

   ex. when key is `:namespace/key` will dispatch on `:namespace/*` otherwise `key`"
  [elem key value]
  (if-let [n (namespace key)] (keyword n "*") key))
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
    "Returns the hoplon managed kids atom, or creates it if missing exist.")
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
           (do-watch kids (partial merge-kids this))))))
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

(defn- add-attributes!
  [this attr]
  (reduce-kv #(do (-attribute! %2 %1 %3) %1) this attr))

(defn- add-children!
  [this [child-cell & _ :as kids]]
  (with-let [this this]
    (doseq [x (vflatten kids)]
      (when-let [x (->node x)]
        (-append-child! this x)))))

(defn- invoke!
  [this & args]
  (let [[attr kids] (parse-args args)]
    (doto (->hoplon this)
      (add-attributes! attr)
      (add-children! kids))))
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
(defn- mksingleton [elem]
  "Retrieves the DOM element `elem` from js/document and updates in-place.
  Creates the element if missing."
  (fn [& args]
   (let [oelem (obj/get js/document elem)]
     (when-not oelem
       (obj/set js/document elem
         (.createElement js/document elem)))
     (with-let [helem (->hoplon oelem)]
       (let [[attrs kids] (parse-args args)]
         (when-not (:static attrs)
           (merge-kids helem nil nil)
           (add-attributes! helem attrs)
           (add-children! helem kids)))))))

(defn- mkelem [tag]
  "Creates a DOM element of `tag` type and upgrades it to a Hoplon Element."
  (fn [& args]
    (let [[attr kids] (parse-args args)
          elem (.createElement js/document tag)
          hl (->hoplon elem)]
      (hl attr kids))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; HTML Elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn html [& args]
 "Updates and returns the document's `html` element in place."
 (with-let [el (.-documentElement js/document)]
  (add-attributes! (->hoplon el) (first (parse-args args)))))

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
(def dialog         (mkelem "dialog")) ;; experimental
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
(def hgroup         (mkelem "hgroup")) ;; experimental
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
(def menu           (mkelem "menu")) ;; experimental
(def menuitem       (mkelem "menuitem")) ;; experimental
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
(def picture        (mkelem "picture")) ;; experimental
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

;; Hoplon elem! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti elem! dispatcher :default ::default)

(defmethod elem! ::default
  [elem key value]
  (cond (cell? value) (do-watch value #(-do! elem key %2))
        (fn? value)   (-on! elem key value)
        :else         (-do! elem key value)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon do! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti do! dispatcher :default ::default)

(defmethod do! ::default
  [elem key val]
  (do! elem :attr {key val}))

(defmethod do! :attr
  [elem _ kvs]
  (set-attributes! elem kvs))

(defmethod do! :html/*
  [elem key val]
  (set-attributes! elem val))

(defmethod do! :svg/*
  [elem key val]
  (set-attributes! elem val))

(defmethod do! :css
  [elem _ kvs]
  (set-styles! elem kvs))

(defmethod do! :css/*
  [elem key val]
  (set-styles! elem val))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Hoplon on! Multimethod ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti on! dispatcher :default ::default)

(defmethod on! ::default
  [elem event callback]
  (.addEventListener elem (name event) callback))

(defmethod on! :html/*
  [elem event callback]
  (.addEventListener elem (name event) callback))
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
  (let [on-deck   (atom ())
        items-seq (cell= (seq items))
        ith-item  #(cell= (nth items-seq % nil))
        shift!    #(with-let [x (first @%)] (swap! % rest))]
    (with-let [current (cell [])]
      (do-watch items-seq
        (fn [old-items new-items]
          (let [old  (count old-items)
                new  (count new-items)
                diff (- new old)]
            (cond (pos? diff)
                  (doseq [i (range old new)]
                    (let [e (or (shift! on-deck) (tpl (ith-item i)))]
                      (swap! current conj e)))
                  (neg? diff)
                  (dotimes [_ (- diff)]
                    (let [e (peek @current)]
                      (swap! current pop)
                      (swap! on-deck conj e))))))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
