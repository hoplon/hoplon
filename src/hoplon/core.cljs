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
    [goog.object    :as obj]
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

(def ^:no-doc static-elements
  "This is an internal implementation detail, exposed for the convenience of
  the hoplon.core/static macro. Experimental."
  (-> #(assoc %1 (.getAttribute %2 "static-id") %2)
      (reduce {} (.querySelector js/document "[static-id]"))))

;;;; public helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn do-watch
  "Public helper.
  Adds f as a watcher to ref and evaluates (f init @ref) once. The watcher
  f is a function of two arguments: the previous and next values. If init is
  not provided the default (nil) will be used."
  ([ref f]
   (do-watch ref nil f))
  ([ref init f]
   (with-let [k (gensym)]
     (f init @ref)
     (add-watch ref k (fn [_ _ old new] (f old new))))))

(defn bust-cache
  "Public helper.
  Experimental."
  [path]
  (let [[f & more] (reverse (split path #"/"))
        [f1 f2]    (split f #"\." 2)]
    (->> [(str f1 "." (cache-key)) f2]
         (join ".")
         (conj more)
         (reverse)
         (join "/"))))

(defn normalize-class
  "Public helper.
  Class normalization for attribute providers."
  [kvs]
  (let [->map #(zipmap % (repeat true))]
    (if (map? kvs)
      kvs
      (->map (if (string? kvs) (.split kvs #"\s+") (seq kvs))))))

;;;; internal helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- child-vec
  [this]
  (let [x (.-childNodes this)
        l (.-length x)]
    (loop [i 0 ret (transient [])]
      (or (and (= i l) (persistent! ret))
          (recur (inc i) (conj! ret (.item x i)))))))

;;;; custom nodes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol INode
  (node [this]))

(extend-type string
  INode
  (node [this]
    ($text this)))

(extend-type number
  INode
  (node [this]
    ($text (str this))))

(defn- ->node
  [x]
  (if (satisfies? INode x) (node x) x))

;;;; custom elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private removeChild  (.. js/Element -prototype -removeChild))
(def ^:private appendChild  (.. js/Element -prototype -appendChild))
(def ^:private insertBefore (.. js/Element -prototype -insertBefore))
(def ^:private replaceChild (.. js/Element -prototype -replaceChild))
(def ^:private setAttribute (.. js/Element -prototype -setAttribute))

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

;;;; custom attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ICustomAttribute
  (-attr! [this elem value]))

(defn attribute? [this]
  (satisfies? ICustomAttribute this))

(extend-type Keyword
  ICustomAttribute
  (-attr! [this elem value]
    (cond (cell? value) (do-watch value #(do! elem this %2))
          (fn? value)   (on! elem this value)
          :else         (do! elem this value))))


;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private is-ie8 (not (obj/get js/window "Node")))

(def ^:private -head*
  (if-not is-ie8
    #(.-head %)
    #(.. % -documentElement -firstChild)))

(def ^:private vector?*
  (if-not is-ie8
    vector?
    #(try (vector? %) (catch js/Error _))))

(def ^:private seq?*
  (if-not is-ie8
    seq?
    #(try (seq? %) (catch js/Error _))))

(defn safe-nth
  "Like cljs.core/nth but returns nil or not found if the index is outside the coll"
  ([coll index] (safe-nth coll index nil))
  ([coll index not-found]
   (try (nth coll index not-found) (catch js/Error _ not-found))))

(defn timeout
  "Executes a fuction after a delay, if no delay is passed, 0 is used as a default."
  ([f] (timeout f 0))
  ([f t] (.setTimeout js/window f t)))

(defn when-dom
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

;; env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-args
  [args]
  (loop [attr (transient {})
         kids (transient [])
         [arg & args] args]
    (if-not arg
      [(persistent! attr) (persistent! kids)]
      (cond (map? arg)       (recur (reduce-kv #(assoc! %1 %2 %3) attr arg) kids args)
            (attribute? arg) (recur (assoc! attr arg (first args)) kids (rest args))
            (seq?* arg)      (recur attr (reduce conj! kids (flatten arg)) args)
            (vector?* arg)   (recur attr (reduce conj! kids (flatten arg)) args)
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
  ICustomElement
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
         (obj/set (.. e -style) (name k) (str v))))))
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
  [elem]
  (fn [& args]
    (let [[attrs kids] (parse-args args)]
      (add-attributes! elem attrs)
      (when (not (:static attrs))
        (remove-all-kids! elem)
        (add-children! elem kids)))))

(defn- make-elem-ctor
  [tag]
  (let [mkelem #(-> js/document (.createElement tag) ensure-kids! (apply %&))]
    (if-not is-ie8
      mkelem
      (fn [& args]
        (try (apply mkelem args)
          (catch js/Error _ (apply (make-elem-ctor "div") args)))))))

(defn html [& args]
  (-> (.-documentElement js/document)
      (add-attributes! (nth (parse-args args) 0))))

(def body
  "Creates a singleton `body` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-singleton-ctor (.-body js/document)))

(def head
  "Creates a singleton `head` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-singleton-ctor (-head* js/document)))

(def a
  "Creates an `a` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "a"))

(def abbr
  "Creates an `abbr` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "abbr"))

(def address
  "Creates an `address` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "address"))

(def area
  "Creates an `area` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "area"))

(def article
  "Creates an `article` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "article"))

(def aside
  "Creates an `aside` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "aside"))

(def audio
  "Creates an `audio` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "audio"))

(def b
  "Creates an `b` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "b"))

(def base
  "Creates an `base` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "base"))

(def bdi
  "Creates an `bdi` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "bdi"))

(def bdo
  "Creates an `bdo` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "bdo"))

(def blockquote
  "Creates an `blockquote` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "blockquote"))

(def br
  "Creates an `br` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "br"))

(def button
  "Creates an `button` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "button"))

(def canvas
  "Creates an `canvas` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "canvas"))

(def caption
  "Creates an `caption` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "caption"))

(def cite
  "Creates an `cite` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "cite"))

(def code
  "Creates an `code` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "code"))

(def col
  "Creates an `col` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "col"))

(def colgroup
  "Creates an `colgroup` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "colgroup"))

(def data
  "Creates an `data` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "data"))

(def datalist
  "Creates an `datalist` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "datalist"))

(def dd
  "Creates an `dd` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "dd"))

(def del
  "Creates an `del` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "del"))

(def details
  "Creates an `details` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "details"))

(def dfn
  "Creates an `dfn` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "dfn"))

(def dialog
  "Creates an `dialog` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "dialog")) ;; experimental

(def div
  "Creates an `div` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "div"))

(def dl
  "Creates an `dl` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "dl"))

(def dt
  "Creates an `dt` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "dt"))

(def em
  "Creates an `em` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "em"))

(def embed
  "Creates an `embed` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "embed"))

(def fieldset
  "Creates an `fieldset` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "fieldset"))

(def figcaption
  "Creates an `figcaption` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "figcaption"))

(def figure
  "Creates an `figure` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "figure"))

(def footer
  "Creates an `footer` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "footer"))

(def form
  "Creates an `form` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "form"))

(def h1
  "Creates an `h1` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "h1"))

(def h2
  "Creates an `h2` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "h2"))

(def h3
  "Creates an `h3` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "h3"))

(def h4
  "Creates an `h4` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "h4"))

(def h5
  "Creates an `h5` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "h5"))

(def h6
  "Creates an `h6` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "h6"))

(def header
  "Creates an `header` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "header"))

(def hgroup
  "Creates an `hgroup` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "hgroup")) ;; experimental

(def hr
  "Creates an `hr` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "hr"))

(def i
  "Creates an `i` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "i"))

(def iframe
  "Creates an `iframe` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "iframe"))

(def img
  "Creates an `img` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "img"))

(def input
  "Creates an `input` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "input"))

(def ins
  "Creates an `ins` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "ins"))

(def kbd
  "Creates an `kbd` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "kbd"))

(def keygen
  "Creates an `keygen` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "keygen"))

(def label
  "Creates an `label` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "label"))

(def legend
  "Creates an `legend` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "legend"))

(def li
  "Creates an `li` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "li"))

(def link
  "Creates an `link` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "link"))

(def main
  "Creates an `main` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "main"))

(def html-map
  "Creates an `map` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "map"))

(def mark
  "Creates an `mark` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "mark"))

(def menu
  "Creates an `menu` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "menu")) ;; experimental

(def menuitem
  "Creates an `menuitem` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "menuitem")) ;; experimental

(def html-meta
  "Creates an `meta` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "meta"))

(def meter
  "Creates an `meter` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "meter"))

(def multicol
  "Creates an `multicol` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "multicol"))

(def nav
  "Creates an `nav` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "nav"))

(def noframes
  "Creates an `noframes` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "noframes"))

(def noscript
  "Creates an `noscript` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "noscript"))

(def html-object
  "Creates an `object` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "object"))

(def ol
  "Creates an `ol` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "ol"))

(def optgroup
  "Creates an `optgroup` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "optgroup"))

(def option
  "Creates an `option` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "option"))

(def output
  "Creates an `output` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "output"))

(def p
  "Creates an `p` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "p"))

(def param
  "Creates an `param` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "param"))

(def picture
  "Creates an `picture` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "picture")) ;; experimental

(def pre
  "Creates an `pre` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "pre"))

(def progress
  "Creates an `progress` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "progress"))

(def q
  "Creates an `q` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "q"))

(def rp
  "Creates an `rp` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "rp"))

(def rt
  "Creates an `rt` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "rt"))

(def rtc
  "Creates an `rtc` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "rtc"))

(def ruby
  "Creates an `ruby` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "ruby"))

(def s
  "Creates an `s` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "s"))

(def samp
  "Creates an `samp` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "samp"))

(def script
  "Creates an `script` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "script"))

(def section
  "Creates an `section` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "section"))

(def select
  "Creates an `select` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "select"))

(def shadow
  "Creates an `shadow` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "shadow"))

(def small
  "Creates an `small` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "small"))

(def source
  "Creates an `source` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "source"))

(def span
  "Creates an `span` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "span"))

(def strong
  "Creates an `strong` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "strong"))

(def style
  "Creates an `style` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "style"))

(def sub
  "Creates an `sub` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "sub"))

(def summary
  "Creates an `summary` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "summary"))

(def sup
  "Creates an `sup` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "sup"))

(def table
  "Creates an `table` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "table"))

(def tbody
  "Creates an `tbody` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "tbody"))

(def td
  "Creates an `td` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "td"))

(def template
  "Creates an `template` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "template"))

(def textarea
  "Creates an `textarea` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "textarea"))

(def tfoot
  "Creates an `tfoot` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "tfoot"))

(def th
  "Creates an `th` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "th"))

(def thead
  "Creates an `thead` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "thead"))

(def html-time
  "Creates an `time` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "time"))

(def title
  "Creates an `title` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "title"))

(def tr
  "Creates an `tr` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "tr"))

(def track
  "Creates an `track` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "track"))

(def u
  "Creates an `u` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "u"))

(def ul
  "Creates an `ul` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "ul"))

(def html-var
  "Creates an `var` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "var"))

(def video
  "Creates an `video` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "video"))

(def wbr
  "Creates an `wbr` element.
  You can pass attributes as a map or keyword value pairs (but not both at the
  same time) and children after the attributes."
  (make-elem-ctor "wbr"))

(def spliced
  "Alias to vector"
  vector)

(defn $text
  "Given a string creates a text node"
  [str]
  (.createTextNode js/document str))

(defn $comment
  "Given a string creates a comment node"
  [str]
  (.createComment js/document str))

(def <!--
  "Given a string creates a comment node"
  $comment)

(def -->
  "Closing placeholder for <!-- to be used in html"
  ::-->)

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

(defmethod do! :attr
  [elem _ kvs]
  (set-attributes! elem kvs))

(defmethod do! :css
  [elem _ kvs]
  (set-styles! elem kvs))

(defmulti on!
  (fn [elem key val]
    (if-let [n (namespace key)] (keyword n "*") key)) :default ::default)

(defmethod on! ::default
  [elem event callback]
  (when-dom elem #(.addEventListener elem (name event) callback)))

(defmethod on! :html/*
  [elem event callback]
  (when-dom elem #(.addEventListener elem (name event) callback)))

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
