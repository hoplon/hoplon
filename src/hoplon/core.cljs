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
    [cljsjs.jquery]
    [clojure.set    :refer [difference intersection]]
    [javelin.core   :refer [cell? cell lift destroy-cell!]]
    [cljs.reader    :refer [read-string]]
    [clojure.string :refer [split join blank?]])
  (:require-macros
    [javelin.core   :refer [with-let cell= prop-cell]]
    [hoplon.core    :refer [cache-key with-timeout with-dom]]))

(declare do! on! $text add-children!)

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
      (reduce {} (.get (js/jQuery "[static-id]")))))

;;;; public helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;; internal helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- child-vec
  [this]
  (let [x (.-childNodes this)
        l (.-length x)]
    (loop [i 0 ret (transient [])]
      (or (and (= i l) (persistent! ret))
          (recur (inc i) (conj! ret (.item x i)))))))

(defn- ->node
  [x]
  (cond (string? x) ($text x)
        (number? x) ($text (str x))
        :else       x))

;;;; custom elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private removeChild  (.. js/Element -prototype -removeChild))
(def ^:private appendChild  (.. js/Element -prototype -appendChild))
(def ^:private insertBefore (.. js/Element -prototype -insertBefore))
(def ^:private replaceChild (.. js/Element -prototype -replaceChild))
(def ^:private setAttribute (.. js/Element -prototype -setAttribute))

(defn- merge-kids
  [this old new]
  (let [new  (remove nil? (flatten new))
        new? (set new)]
    (loop [[x & xs] new
           [k & ks :as kids] (child-vec this)]
      (when (or x k)
        (recur xs (cond (= x k) ks
                        (not k) (with-let [ks ks]
                                  (.call appendChild this (->node x)))
                        (not x) (with-let [ks ks]
                                  (when-not (new? k)
                                    (.call removeChild this (->node k))))
                        :else   (with-let [kids kids]
                                  (.call insertBefore this (->node x) (->node k)))))))))

(defn ensure-kids!
  [this]
  (with-let [this this]
    (when-not (.-hoplonKids this)
      (let [kids (atom (child-vec this))]
        (set! (.-hoplonKids this) kids)
        (do-watch kids (partial merge-kids this))))))

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

;;;; custom elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ICustomElement
  (-set-attributes! [this kvs])
  (-set-styles!     [this kvs])
  (-append-child!   [this child])
  (-remove-child!   [this child])
  (-replace-child!  [this new existing])
  (-insert-before!  [this new existing]))

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

;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private is-ie8 (not (aget js/window "Node")))

(def ^:private node?
  (if-not is-ie8
    #(instance? js/Node %)
    #(try (.-nodeType %) (catch js/Error _))))

(def ^:private vector?*
  (if-not is-ie8
    vector?
    #(try (vector? %) (catch js/Error _))))

(def ^:private seq?*
  (if-not is-ie8
    seq?
    #(try (seq? %) (catch js/Error _))))

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

;; env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-args
  [args]
  (loop [attr (transient {})
         kids (transient [])
         [arg & args] args]
    (if-not arg
      [(persistent! attr) (persistent! kids)]
      (cond (map? arg)     (recur (reduce-kv #(assoc! %1 %2 %3) attr arg) kids args)
            (keyword? arg) (recur (assoc! attr arg (first args)) kids (rest args))
            (seq?* arg)    (recur attr (reduce conj! kids (flatten arg)) args)
            (vector?* arg) (recur attr (reduce conj! kids (flatten arg)) args)
            :else          (recur attr (conj! kids arg) args)))))

(defn- add-attributes!
  [this attr]
  (with-let [this this]
    (-> (fn [this k v]
          (with-let [this this]
            (cond (cell? v) (do-watch v #(do! this k %2))
                  (fn? v)   (on! this k v)
                  :else     (do! this k v))))
        (reduce-kv this attr))))

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
  ICustomElement
  (-set-attributes!
    ([this kvs]
     (let [e (js/jQuery this)]
       (doseq [[k v] kvs :let [k (name k)]]
         (if (= false v)
           (.removeAttr e k)
           (.attr e k (if (= true v) k v)))))))
  (-set-styles!
    ([this kvs]
     (let [e (js/jQuery this)]
       (doseq [[k v] kvs]
         (.css e (name k) (str v))))))
  (-append-child!
    ([this child]
     (if-not is-ie8
       (.appendChild this child)
       (try (.appendChild this child) (catch js/Error _)))))
  (-remove-child!
    ([this child]
     (.removeChild this child)))
  (-replace-child!
    ([this new existing]
     (.replaceChild this new existing)))
  (-insert-before!
    ([this new existing]
     (.insertBefore this new existing))))

(defn- make-singleton-ctor
  [tag]
  (fn [& args]
    (let [[attrs kids] (parse-args args)
          elem         (-> js/document
                           (.getElementsByTagName tag)
                           (aget 0))]
      (add-attributes! elem attrs)
      (when (not (:static attrs))
        (set! (.-hoplonKids elem) nil)
        (set! (.-innerHTML  elem) "")
        (add-children! elem kids)))))

(defn- make-elem-ctor
  [tag]
  (fn [& args]
    (-> js/document (.createElement tag) ensure-kids! (apply args))))

(defn html
  [& args]
  (let [[attrs _] (parse-args args)]
    (-> (.getElementsByTagName js/document "html")
        (aget 0)
        (add-attributes! attrs))))

(def body           (make-singleton-ctor "body"))
(def head           (make-singleton-ctor "head"))
(def a              (make-elem-ctor "a"))
(def abbr           (make-elem-ctor "abbr"))
(def acronym        (make-elem-ctor "acronym"))
(def address        (make-elem-ctor "address"))
(def applet         (make-elem-ctor "applet"))
(def area           (make-elem-ctor "area"))
(def article        (make-elem-ctor "article"))
(def aside          (make-elem-ctor "aside"))
(def audio          (make-elem-ctor "audio"))
(def b              (make-elem-ctor "b"))
(def base           (make-elem-ctor "base"))
(def basefont       (make-elem-ctor "basefont"))
(def bdi            (make-elem-ctor "bdi"))
(def bdo            (make-elem-ctor "bdo"))
(def big            (make-elem-ctor "big"))
(def blockquote     (make-elem-ctor "blockquote"))
(def br             (make-elem-ctor "br"))
(def button         (make-elem-ctor "button"))
(def canvas         (make-elem-ctor "canvas"))
(def caption        (make-elem-ctor "caption"))
(def center         (make-elem-ctor "center"))
(def cite           (make-elem-ctor "cite"))
(def code           (make-elem-ctor "code"))
(def col            (make-elem-ctor "col"))
(def colgroup       (make-elem-ctor "colgroup"))
(def command        (make-elem-ctor "command"))
(def data           (make-elem-ctor "data"))
(def datalist       (make-elem-ctor "datalist"))
(def dd             (make-elem-ctor "dd"))
(def del            (make-elem-ctor "del"))
(def details        (make-elem-ctor "details"))
(def dfn            (make-elem-ctor "dfn"))
(def dialog         (make-elem-ctor "dialog"))
(def dir            (make-elem-ctor "dir"))
(def div            (make-elem-ctor "div"))
(def dl             (make-elem-ctor "dl"))
(def dt             (make-elem-ctor "dt"))
(def em             (make-elem-ctor "em"))
(def embed          (make-elem-ctor "embed"))
(def eventsource    (make-elem-ctor "eventsource"))
(def fieldset       (make-elem-ctor "fieldset"))
(def figcaption     (make-elem-ctor "figcaption"))
(def figure         (make-elem-ctor "figure"))
(def font           (make-elem-ctor "font"))
(def footer         (make-elem-ctor "footer"))
(def form           (make-elem-ctor "form"))
(def frame          (make-elem-ctor "frame"))
(def frameset       (make-elem-ctor "frameset"))
(def h1             (make-elem-ctor "h1"))
(def h2             (make-elem-ctor "h2"))
(def h3             (make-elem-ctor "h3"))
(def h4             (make-elem-ctor "h4"))
(def h5             (make-elem-ctor "h5"))
(def h6             (make-elem-ctor "h6"))
(def header         (make-elem-ctor "header"))
(def hgroup         (make-elem-ctor "hgroup"))
(def hr             (make-elem-ctor "hr"))
(def i              (make-elem-ctor "i"))
(def iframe         (make-elem-ctor "iframe"))
(def img            (make-elem-ctor "img"))
(def input          (make-elem-ctor "input"))
(def ins            (make-elem-ctor "ins"))
(def isindex        (make-elem-ctor "isindex"))
(def kbd            (make-elem-ctor "kbd"))
(def keygen         (make-elem-ctor "keygen"))
(def label          (make-elem-ctor "label"))
(def legend         (make-elem-ctor "legend"))
(def li             (make-elem-ctor "li"))
(def link           (make-elem-ctor "link"))
(def main           (make-elem-ctor "main"))
(def html-map       (make-elem-ctor "map"))
(def mark           (make-elem-ctor "mark"))
(def menu           (make-elem-ctor "menu"))
(def menuitem       (make-elem-ctor "menuitem"))
(def html-meta      (make-elem-ctor "meta"))
(def meter          (make-elem-ctor "meter"))
(def nav            (make-elem-ctor "nav"))
(def noframes       (make-elem-ctor "noframes"))
(def noscript       (make-elem-ctor "noscript"))
(def object         (make-elem-ctor "object"))
(def ol             (make-elem-ctor "ol"))
(def optgroup       (make-elem-ctor "optgroup"))
(def option         (make-elem-ctor "option"))
(def output         (make-elem-ctor "output"))
(def p              (make-elem-ctor "p"))
(def param          (make-elem-ctor "param"))
(def pre            (make-elem-ctor "pre"))
(def progress       (make-elem-ctor "progress"))
(def q              (make-elem-ctor "q"))
(def rp             (make-elem-ctor "rp"))
(def rt             (make-elem-ctor "rt"))
(def rtc            (make-elem-ctor "rtc"))
(def ruby           (make-elem-ctor "ruby"))
(def s              (make-elem-ctor "s"))
(def samp           (make-elem-ctor "samp"))
(def script         (make-elem-ctor "script"))
(def section        (make-elem-ctor "section"))
(def select         (make-elem-ctor "select"))
(def small          (make-elem-ctor "small"))
(def source         (make-elem-ctor "source"))
(def span           (make-elem-ctor "span"))
(def strike         (make-elem-ctor "strike"))
(def strong         (make-elem-ctor "strong"))
(def style          (make-elem-ctor "style"))
(def sub            (make-elem-ctor "sub"))
(def summary        (make-elem-ctor "summary"))
(def sup            (make-elem-ctor "sup"))
(def table          (make-elem-ctor "table"))
(def tbody          (make-elem-ctor "tbody"))
(def td             (make-elem-ctor "td"))
(def textarea       (make-elem-ctor "textarea"))
(def tfoot          (make-elem-ctor "tfoot"))
(def th             (make-elem-ctor "th"))
(def thead          (make-elem-ctor "thead"))
(def html-time      (make-elem-ctor "time"))
(def title          (make-elem-ctor "title"))
(def tr             (make-elem-ctor "tr"))
(def track          (make-elem-ctor "track"))
(def tt             (make-elem-ctor "tt"))
(def u              (make-elem-ctor "u"))
(def ul             (make-elem-ctor "ul"))
(def html-var       (make-elem-ctor "var"))
(def video          (make-elem-ctor "video"))
(def wbr            (make-elem-ctor "wbr"))

(def spliced        vector)
(def $text          #(.createTextNode js/document %))
(def $comment       #(.createComment js/document %))

(def <!--           $comment)
(def -->            ::-->)

(defn add-initfn!  [f] (js/jQuery #(with-timeout 0 (f))))
(defn page-load    []  (.trigger (js/jQuery js/document) "page-load"))
(defn on-page-load [f] (.on (js/jQuery js/document) "page-load" f))

(add-initfn!
  (fn []
    (. (js/jQuery "body")
       (on "submit"
           #(let [e (js/jQuery (.-target %))]
              (when-not (or (.attr e "action") (.attr e "method"))
                (.preventDefault %)))))))

;; frp helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn text-val!
  ([e] (.val e))
  ([e v] (let [v (str v)]
           (when (not= v (text-val! e))
             (.val e v)))))

(defn check-val!
  ([e] (.is e ":checked"))
  ([e v] (.prop e "checked" (boolean v))))

;; custom attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti do!
  (fn [elem key val]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(defmethod do! ::default
  [elem key val]
  (do! elem :attr {key val}))

(defmethod do! :css/*
  [elem key val]
  (set-styles! elem key val))

(defmethod do! :html/*
  [elem key val]
  (set-attributes! elem key val))

(defmethod do! :svg/*
  [elem key val]
  (set-attributes! elem key val))

(defmethod do! :value
  [elem _ & args]
  (let [e (js/jQuery elem)]
    (apply (if (= "checkbox" (.attr e "type")) check-val! text-val!) e args)))

(defmethod do! :attr
  [elem _ kvs]
  (set-attributes! elem kvs))

(defmethod do! :class
  [elem _ kvs]
  (let [elem  (js/jQuery elem)
        ->map #(zipmap % (repeat true))
        clmap (if (map? kvs)
                kvs
                (->map (if (string? kvs) (.split kvs #"\s+") (seq kvs))))]
    (doseq [[c p?] clmap] (.toggleClass elem (name c) (boolean p?)))))

(defmethod do! :css
  [elem _ kvs]
  (set-styles! elem kvs))

(defmethod do! :toggle
  [elem _ v]
  (.toggle (js/jQuery elem) (boolean v)))

(defmethod do! :slide-toggle
  [elem _ v]
  (if v
    (.slideDown (.hide (js/jQuery elem)) "fast")
    (.slideUp (js/jQuery elem) "fast")))

(defmethod do! :fade-toggle
  [elem _ v]
  (if v
    (.fadeIn (.hide (js/jQuery elem)) "fast")
    (.fadeOut (js/jQuery elem) "fast")))

(defmethod do! :focus
  [elem _ v]
  (with-timeout 0
    (if v (.focus (js/jQuery elem)) (.focusout (js/jQuery elem)))))

(defmethod do! :select
  [elem _ _]
  (.select (js/jQuery elem)))

(defmethod do! :focus-select
  [elem _ v]
  (when v (do! elem :focus v) (do! elem :select v)))

(defmethod do! :text
  [elem _ v]
  (.text (js/jQuery elem) (str v)))

(defmethod do! :html
  [elem _ v]
  (.html (js/jQuery elem) v))

(defmethod do! :scroll-to
  [elem _ v]
  (when v
    (let [body (js/jQuery "body,html")
          elem (js/jQuery elem)]
      (.animate body (clj->js {:scrollTop (.-top (.offset elem))})))))

(defmulti on!
  (fn [elem key val]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(extend-type js/jQuery.Event
  cljs.core/IDeref
  (-deref [this] (-> this .-target js/jQuery .val)))

(defmethod on! ::default
  [elem event callback]
  (when-dom elem #(.on (js/jQuery elem) (name event) callback)))

(defmethod on! :html/*
  [elem event callback]
  (when-dom elem #(.on (js/jQuery elem) (name event) callback)))

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
      (-> (js/jQuery js/window)
          (.on "hashchange" #(reset! c (.. js/window -location -hash)))))))
