;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns hoplon.core
  (:require
    [goog.Uri]
    [goog.object     :as obj]
    [clojure.set     :refer [difference intersection]]
    [javelin.core    :refer [cell? cell lift destroy-cell!]]
    [cljs.reader     :refer [read-string]]
    [clojure.string  :refer [split join blank?]]
    [hoplon.protocol :refer [IHoplonNode IHoplonConstructor IHoplonElement IHoplonAttribute
                             -node -ctor! -set-attributes! -set-styles! -append-child!
                             -remove-child! -replace-child! -insert-before! -attr!]])
  (:require-macros
    [javelin.core    :refer [with-let cell= prop-cell]]
    [hoplon.core     :refer [cache-key with-timeout with-dom]]))

(declare mk! do! on! $text add-children!)

(enable-console-print!)

(def prerendering?
  "Is the application running in a prerendering container (eg. PhantomJS via
  the prerender task)?"
  (.getParameterValue (goog.Uri. (.. js/window -location -href)) "prerendering"))

;; This is an internal implementation detail, exposed for the convenience of
;; the hoplon.core/static macro.
(def static-elements
  "Experimental."
  (-> #(assoc %1 (.getAttribute %2 "static-id") %2)
      (reduce {} (.querySelector js/document "[static-id]"))))

;; Public Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn bust-cache
  "Experimental."
  [path]
  (let [[f & more] (reverse (split path #"/"))
        [f1 f2]    (split f #"\." 2)]
    (->> [(str f1 "." (cache-key)) f2]
         (join ".")
         (conj more)
         (reverse)
         (join "/"))))

(defn normalize-class
  "Class normalization for attribute providers."
  [kvs]
  (let [->map #(zipmap % (repeat true))]
    (if (map? kvs)
      kvs
      (->map (if (string? kvs) (.split kvs #"\s+") (seq kvs))))))

(defn safe-nth
  ([coll index] (safe-nth coll index nil))
  ([coll index not-found]
   (try (nth coll index not-found) (catch js/Error _ not-found))))

(defn timeout
  ([f] (timeout f 0))
  ([f t] (.setTimeout js/window f t)))

(defn when-dom [this f]
  (if-not (instance? js/Element this)
    (f)
    (timeout
      (fn doit []
        (if (.contains (.-documentElement js/document) this) (f) (timeout doit 20))))))

;; Hoplon Constructor ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol IHoplonConstructor
  string
  (-ctor!
    ([this]
      (mk! this ::default))
    ([this key]
      (mk! this key)))
  object
  (-ctor!
    ([this]
      (mk! this ::default))
    ([this key]
      (mk! this key)))
  js/Element
  (-ctor!
    ([this]
      (mk! this :elem))
    ([this key]
      (mk! this key))))

;; Hoplon Nodes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol IHoplonNode
  string
  (-node [this]
    ($text this))
  number
  (-node [this]
    ($text (str this))))

(defn- ->node [x] (if (satisfies? IHoplonNode x) (-node x) x))

;; Hoplon Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn attribute? [this]
  (satisfies? IHoplonAttribute this))

(extend-protocol IHoplonAttribute
  Keyword
  (-attr! [this elem value]
    (cond (cell? value) (do-watch value #(do! elem this %2))
          (fn? value)   (on! elem this value)
          :else         (do! elem this value))))

;; Hoplon Element Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private removeChild  (.. js/Element -prototype -removeChild))
(def ^:private appendChild  (.. js/Element -prototype -appendChild))
(def ^:private insertBefore (.. js/Element -prototype -insertBefore))
(def ^:private replaceChild (.. js/Element -prototype -replaceChild))
(def ^:private setAttribute (.. js/Element -prototype -setAttribute))

(defn- child-vec
  [this]
  (let [x (.-childNodes this)
        l (.-length x)]
    (loop [i 0 ret (transient [])]
      (or (and (= i l) (persistent! ret))
          (recur (inc i) (conj! ret (.item x i)))))))

(defn- merge-kids
  [this _ new]
  (let [new  (->> (flatten new) (remove nil?) (map ->node))
        new? (set new)]
    (loop [[x & xs] new
           [k & ks :as kids] (child-vec this)]
      (when (or x k)
        (recur xs (cond (= x k) ks
                        (not k) (with-let [ks ks]
                                  (.call appendChild this x))
                        (not x) (with-let [ks ks]
                                  (when-not (new? k)
                                    (.call removeChild this k)))
                        :else   (with-let [kids kids]
                                  (.call insertBefore this x k))))))))

(defn- ensure-kids!
  [this]
  (with-let [this this]
    (when-not (.-hoplonKids this)
      (let [kids (atom (child-vec this))]
        (set! (.-hoplonKids this) kids)
        (do-watch kids (partial merge-kids this))))))

(defn- remove-all-kids!
  [this]
  (set! (.-hoplonKids this) nil)
  (merge-kids this nil nil))

(defn- set-appendChild!
  [this kidfn]
  (set! (.-appendChild this)
        (fn [x]
          (this-as this
            (with-let [x x]
              (ensure-kids! this)
              (let [kids (kidfn this)
                    i    (count @kids)]
                (if (cell? x)
                  (do-watch x #(swap! kids assoc i %2))
                  (swap! kids assoc i x))))))))

(defn- set-removeChild!
  [this kidfn]
  (set! (.-removeChild this)
        (fn [x]
          (this-as this
            (with-let [x x]
              (ensure-kids! this)
              (swap! (kidfn this) #(into [] (remove (partial = x) %))))))))

(defn- set-insertBefore!
  [this kidfn]
  (set! (.-insertBefore this)
        (fn [x y]
          (this-as this
            (with-let [x x]
              (ensure-kids! this)
              (cond
                (not y)     (swap! (kidfn this) conj x)
                (not= x y)  (swap! (kidfn this) #(vec (mapcat (fn [z] (if (= z y) [x z] [z])) %)))))))))

(defn- set-replaceChild!
  [this kidfn]
  (set! (.-replaceChild this)
        (fn [x y]
          (this-as this
            (with-let [y y]
              (ensure-kids! this)
              (swap! (kidfn this) #(mapv (fn [z] (if (= z y) x z)) %)))))))

(defn- set-setAttribute!
  [this attrfn]
  (set! (.-setAttribute this)
        (fn [k v]
          (this-as this
            (with-let [_ js/undefined]
              (let [kk   (keyword k)
                    attr (attrfn this)
                    has? (and attr (contains? @attr kk))]
                (if has?
                  (swap! attr assoc kk v)
                  (.call setAttribute this k v))))))))

(set-appendChild!  (.-prototype js/Element) #(.-hoplonKids %))
(set-removeChild!  (.-prototype js/Element) #(.-hoplonKids %))
(set-insertBefore! (.-prototype js/Element) #(.-hoplonKids %))
(set-replaceChild! (.-prototype js/Element) #(.-hoplonKids %))

;; Hoplon Element Fns ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mk-elem!
  ([elem]
    (-ctor! elem))
  ([elem key]
    (-ctor! elem key)))

(defn set-attributes!
  ([this kvs]
   (-set-attributes! this kvs))
  ([this k v & kvs]
   (set-attributes! this (apply hash-map k v kvs))))

(defn set-styles!
  ([this kvs]
   (-set-styles! this kvs))
  ([this k v & kvs]
   (set-styles! this (apply hash-map k v kvs))))

(defn append-child!
  [this child]
  (-append-child! this child))

(defn remove-child!
  [this child]
  (-remove-child! this child))

(defn replace-child!
  [this new existing]
  (-replace-child! this new existing))

(defn insert-before!
  [this new existing]
  (-insert-before! this new existing))

;; Hoplon Element ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-args
  [args]
  (loop [attr (transient {})
         kids (transient [])
         [arg & args] args]
    (if-not arg
      [(persistent! attr) (persistent! kids)]
      (cond (map? arg)       (recur (reduce-kv #(assoc! %1 %2 %3) attr arg) kids args)
            (attribute? arg) (recur (assoc! attr arg (first args)) kids (rest args))
            (seq? arg)      (recur attr (reduce conj! kids (flatten arg)) args)
            (vector? arg)   (recur attr (reduce conj! kids (flatten arg)) args)
            :else            (recur attr (conj! kids arg) args)))))

(defn- add-attributes!
  [this attr]
  (reduce-kv #(do (-attr! %2 %1 %3) %1) this attr))

(defn- add-children!
  [this [child-cell & _ :as kids]]
  (with-let [this this]
    (doseq [x (flatten kids)]
      (when-let [x (->node x)]
        (append-child! this x)))))

(extend-type js/Element
  IPrintWithWriter
  (-pr-writer
    ([this writer opts]
     (write-all writer "#<Element: " (.-tagName this) ">")))
  IFn
  (-invoke
    ([this & args]
     (let [[attr kids] (parse-args args)]
       (doto this
         (add-attributes! attr)
         (add-children! kids)))))
  IHoplonElement
  (-set-attributes!
    ([this kvs]
     (let [e this]
       (doseq [[k v] kvs :let [k (name k)]]
         (if (= false v)
           (.removeAttribute e k)
           (.setAttribute e k (if (= true v) k v)))))))
  (-set-styles!
    ([this kvs]
     (let [e this]
       (doseq [[k v] kvs]
         (obj/set e "style" (name k) (str v))))))
  (-append-child!
    ([this child]
     (.appendChild this child)))
  (-remove-child!
    ([this child]
     (.removeChild this child)))
  (-replace-child!
    ([this new existing]
     (.replaceChild this new existing)))
  (-insert-before!
    ([this new existing]
     (.insertBefore this new existing))))

;; Hoplon Multimethods ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti mk!
  (fn [elem key]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(defmulti do!
  (fn [elem key val]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(defmulti on!
  (fn [elem key val]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

;; Hoplon Multimethod Defaults ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod mk! ::default
  [elem _]
  (mk! elem :tag))

(defmethod do! ::default
  [elem key val]
  (do! elem :attr {key val}))

(defmethod on! ::default
  [elem event callback]
  (when-dom elem #(.addEventListener elem (name event) callback)))

;; Hoplon Constructors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod mk! :elem
  [elem _]
  (fn [& args]
    (let [[attrs kids] (parse-args args)]
      (add-attributes! elem attrs)
      (when (not (:static attrs))
        (remove-all-kids! elem)
        (add-children! elem kids)))))

(defmethod mk! :tag
  [tag _]
  #(-> js/document (.createElement tag) ensure-kids! (apply %&)))

(defmethod mk! :html
  [elem _]
  (fn [& args]
    (add-attributes! (.. elem -documentElement) (nth (parse-args args) 0))))

(defmethod mk! :head
  [elem _]
  (mk! (.-head elem) :elem))

(defmethod mk! :body
  [elem _]
  (mk! (.-body elem) :elem))

;; Hoplon Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod do! :css/*
  [elem key val]
  (set-styles! elem key val))

(defmethod do! :html/*
  [elem key val]
  (set-attributes! elem key val))

(defmethod do! :svg/*
  [elem key val]
  (set-attributes! elem key val))

(defmethod do! :attr
  [elem _ kvs]
  (set-attributes! elem kvs))

(defmethod do! :css
  [elem _ kvs]
  (set-styles! elem kvs))

(defmethod on! :html/*
  [elem event callback]
  (when-dom elem #(.addEventListener elem (name event) callback)))

;; Hoplon DOM Elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def html           (mk-elem! js/document :html))

(def head           (mk-elem! js/document :head))
(def body           (mk-elem! js/document :body))

(def a              (mk-elem! "a"))
(def abbr           (mk-elem! "abbr"))
(def address        (mk-elem! "address"))
(def area           (mk-elem! "area"))
(def article        (mk-elem! "article"))
(def aside          (mk-elem! "aside"))
(def audio          (mk-elem! "audio"))
(def b              (mk-elem! "b"))
(def base           (mk-elem! "base"))
(def bdi            (mk-elem! "bdi"))
(def bdo            (mk-elem! "bdo"))
(def blockquote     (mk-elem! "blockquote"))
(def br             (mk-elem! "br"))
(def button         (mk-elem! "button"))
(def canvas         (mk-elem! "canvas"))
(def caption        (mk-elem! "caption"))
(def cite           (mk-elem! "cite"))
(def code           (mk-elem! "code"))
(def col            (mk-elem! "col"))
(def colgroup       (mk-elem! "colgroup"))
(def data           (mk-elem! "data"))
(def datalist       (mk-elem! "datalist"))
(def dd             (mk-elem! "dd"))
(def del            (mk-elem! "del"))
(def details        (mk-elem! "details"))
(def dfn            (mk-elem! "dfn"))
(def dialog         (mk-elem! "dialog")) ;; experimental
(def div            (mk-elem! "div"))
(def dl             (mk-elem! "dl"))
(def dt             (mk-elem! "dt"))
(def em             (mk-elem! "em"))
(def embed          (mk-elem! "embed"))
(def fieldset       (mk-elem! "fieldset"))
(def figcaption     (mk-elem! "figcaption"))
(def figure         (mk-elem! "figure"))
(def footer         (mk-elem! "footer"))
(def form           (mk-elem! "form"))
(def h1             (mk-elem! "h1"))
(def h2             (mk-elem! "h2"))
(def h3             (mk-elem! "h3"))
(def h4             (mk-elem! "h4"))
(def h5             (mk-elem! "h5"))
(def h6             (mk-elem! "h6"))
(def header         (mk-elem! "header"))
(def hgroup         (mk-elem! "hgroup")) ;; experimental
(def hr             (mk-elem! "hr"))
(def i              (mk-elem! "i"))
(def iframe         (mk-elem! "iframe"))
(def img            (mk-elem! "img"))
(def input          (mk-elem! "input"))
(def ins            (mk-elem! "ins"))
(def kbd            (mk-elem! "kbd"))
(def keygen         (mk-elem! "keygen"))
(def label          (mk-elem! "label"))
(def legend         (mk-elem! "legend"))
(def li             (mk-elem! "li"))
(def link           (mk-elem! "link"))
(def main           (mk-elem! "main"))
(def html-map       (mk-elem! "map"))
(def mark           (mk-elem! "mark"))
(def menu           (mk-elem! "menu")) ;; experimental
(def menuitem       (mk-elem! "menuitem")) ;; experimental
(def html-meta      (mk-elem! "meta"))
(def meter          (mk-elem! "meter"))
(def multicol       (mk-elem! "multicol"))
(def nav            (mk-elem! "nav"))
(def noframes       (mk-elem! "noframes"))
(def noscript       (mk-elem! "noscript"))
(def html-object    (mk-elem! "object"))
(def ol             (mk-elem! "ol"))
(def optgroup       (mk-elem! "optgroup"))
(def option         (mk-elem! "option"))
(def output         (mk-elem! "output"))
(def p              (mk-elem! "p"))
(def param          (mk-elem! "param"))
(def picture        (mk-elem! "picture")) ;; experimental
(def pre            (mk-elem! "pre"))
(def progress       (mk-elem! "progress"))
(def q              (mk-elem! "q"))
(def rp             (mk-elem! "rp"))
(def rt             (mk-elem! "rt"))
(def rtc            (mk-elem! "rtc"))
(def ruby           (mk-elem! "ruby"))
(def s              (mk-elem! "s"))
(def samp           (mk-elem! "samp"))
(def script         (mk-elem! "script"))
(def section        (mk-elem! "section"))
(def select         (mk-elem! "select"))
(def shadow         (mk-elem! "shadow"))
(def small          (mk-elem! "small"))
(def source         (mk-elem! "source"))
(def span           (mk-elem! "span"))
(def strong         (mk-elem! "strong"))
(def style          (mk-elem! "style"))
(def sub            (mk-elem! "sub"))
(def summary        (mk-elem! "summary"))
(def sup            (mk-elem! "sup"))
(def table          (mk-elem! "table"))
(def tbody          (mk-elem! "tbody"))
(def td             (mk-elem! "td"))
(def template       (mk-elem! "template"))
(def textarea       (mk-elem! "textarea"))
(def tfoot          (mk-elem! "tfoot"))
(def th             (mk-elem! "th"))
(def thead          (mk-elem! "thead"))
(def html-time      (mk-elem! "time"))
(def title          (mk-elem! "title"))
(def tr             (mk-elem! "tr"))
(def track          (mk-elem! "track"))
(def u              (mk-elem! "u"))
(def ul             (mk-elem! "ul"))
(def html-var       (mk-elem! "var"))
(def video          (mk-elem! "video"))
(def wbr            (mk-elem! "wbr"))

(def spliced        vector)
(def $text          #(.createTextNode js/document %))
(def $comment       #(.createComment js/document %))

(def <!--           $comment)
(def -->            ::-->)

(defn add-initfn!  [f] (.addEventListener js/window "load" #(with-timeout 0 (f))))
(defn page-load    []  (.dispatchEvent js/document "page-load"))
(defn on-page-load [f] (.addEventListener js/document "page-load" f))

(add-initfn!
  (fn []
    (. (.-body js/document)
       (addEventListener "submit"
           #(let [e (.-target %)]
              (when-not (or (.getAttribute e "action") (.getAttribute e "method"))
                (.preventDefault %)))))))

;; Hoplon Cells ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn loop-tpl*
  "Given a cell items containing a seqable collection, constructs a cell that
  works like a fill vector. The template tpl is a function of one argument: the
  formula cell containing the ith item in items. The tpl function is called
  once (and only once) for each index in items. When the items collection
  shrinks the DOM element created by the template is not destroyed--it is only
  removed from the DOM and cached. When the items collection grows again those
  cached elements will be reinserted into the DOM at their original index."
  [items tpl]
  (let [on-deck   (atom ())
        items-seq (cell= (seq items))
        ith-item  #(cell= (safe-nth items-seq %))
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

(defn route-cell
  "Defines a cell whose value is the URI fragment."
  [& [default]]
  (let [c (cell (.. js/window -location -hash))]
    (with-let [_ (cell= (or (and (seq c) c) default))]
      (-> js/window
          (.addEventListener "hashchange" #(reset! c (.. js/window -location -hash)))))))
