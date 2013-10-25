;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon
  (:require-macros
    [tailrecursion.javelin :refer [with-let cell=]])
  (:require
    tailrecursion.javelin
    [goog.dom         :as gdom]
    [clojure.string   :refer [split join blank?]]))

(set-print-fn! #(.log js/console %))

(defn safe-name [x]
  (try (name x) (catch js/Error e)))

(defn safe-nth
  ([coll index] (safe-nth coll index nil))
  ([coll index not-found]
   (try (nth coll index not-found) (catch js/Error _ not-found))))

;; env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- unsplice [forms]
  (mapcat #(if (vector? %) (unsplice %) [%]) forms))

(def DIRECT-ATTRS
  {"cellpadding"  "cellPadding"
   "cellspacing"  "cellSpacing"
   "colspan"      "colSpan"
   "rowspan"      "rowSpan"
   "valign"       "vAlign"
   "height"       "height"
   "width"        "width"
   "frameborder"  "frameBorder"
   "data-hl"      "data-hl"})

(extend-type js/Text
  IPrintWithWriter
  (-pr-writer
    ([this writer opts]
     (write-all writer "#<Text: " (.-nodeValue this) ">"))))

(extend-type js/Element
  IPrintWithWriter
  (-pr-writer
    ([this writer opts]
     (write-all writer "#<Element: " (.-tagName this) ">")))
  IFn
  (-invoke
    ([this & [head & tail :as args]]
     (when (seq args)
       (let [kw1? (comp keyword? first)
             mkkw #(->> (partition 2 %) (take-while kw1?) (map vec))
             drkw #(->> (partition 2 %) (drop-while kw1?) (mapcat identity))
             [attr kids] (cond (map?     head) [head tail]
                               (keyword? head) [(into {} (mkkw args)) (drkw args)]
                               :else           [{} args])]
         (doseq [[k v] attr]
           (when-not (= v false)
             (let [k (name k)
                   v (if (= v true) k (str v))]
               (cond (= k "style")              (set! (.-cssText (.-style this)) v)
                     (= k "class")              (set! (.-className this) v)
                     (= k   "for")              (set! (.-htmlFor this) v)
                     (contains? DIRECT-ATTRS k) (.setAttribute this (DIRECT-ATTRS k) v)
                     :else                      (aset this k v)))))
         (doseq [x (unsplice kids)]
           (let [kid (cond (string? x)            (.createTextNode js/document x)
                           (instance? js/Node x)  x)]
             (when kid (.appendChild this kid)))))) 
     this)))

(defn clone [this] this)

(defn- make-elem [tag]
  (fn [& args]
    (apply (.createElement js/document tag) args)))

(def a              (make-elem "a"))
(def abbr           (make-elem "abbr"))
(def acronym        (make-elem "acronym"))
(def address        (make-elem "address"))
(def applet         (make-elem "applet"))
(def area           (make-elem "area"))
(def article        (make-elem "article"))
(def aside          (make-elem "aside"))
(def audio          (make-elem "audio"))
(def b              (make-elem "b"))
(def base           (make-elem "base"))
(def basefont       (make-elem "basefont"))
(def bdi            (make-elem "bdi"))
(def bdo            (make-elem "bdo"))
(def big            (make-elem "big"))
(def blockquote     (make-elem "blockquote"))
(def body           (make-elem "body"))
(def br             (make-elem "br"))
(def button         (make-elem "button"))
(def canvas         (make-elem "canvas"))
(def caption        (make-elem "caption"))
(def center         (make-elem "center"))
(def cite           (make-elem "cite"))
(def code           (make-elem "code"))
(def col            (make-elem "col"))
(def colgroup       (make-elem "colgroup"))
(def command        (make-elem "command"))
(def data           (make-elem "data"))
(def datalist       (make-elem "datalist"))
(def dd             (make-elem "dd"))
(def del            (make-elem "del"))
(def details        (make-elem "details"))
(def dfn            (make-elem "dfn"))
(def dir            (make-elem "dir"))
(def div            (make-elem "div"))
(def dl             (make-elem "dl"))
(def dt             (make-elem "dt"))
(def em             (make-elem "em"))
(def embed          (make-elem "embed"))
(def eventsource    (make-elem "eventsource"))
(def fieldset       (make-elem "fieldset"))
(def figcaption     (make-elem "figcaption"))
(def figure         (make-elem "figure"))
(def font           (make-elem "font"))
(def footer         (make-elem "footer"))
(def form           (make-elem "form"))
(def frame          (make-elem "frame"))
(def frameset       (make-elem "frameset"))
(def h1             (make-elem "h1"))
(def h2             (make-elem "h2"))
(def h3             (make-elem "h3"))
(def h4             (make-elem "h4"))
(def h5             (make-elem "h5"))
(def h6             (make-elem "h6"))
(def head           (make-elem "head"))
(def header         (make-elem "header"))
(def hgroup         (make-elem "hgroup"))
(def hr             (make-elem "hr"))
(def html           (make-elem "html"))
(def i              (make-elem "i"))
(def iframe         (make-elem "iframe"))
(def img            (make-elem "img"))
(def input          (make-elem "input"))
(def ins            (make-elem "ins"))
(def isindex        (make-elem "isindex"))
(def kbd            (make-elem "kbd"))
(def keygen         (make-elem "keygen"))
(def label          (make-elem "label"))
(def legend         (make-elem "legend"))
(def li             (make-elem "li"))
(def link           (make-elem "link"))
(def html-map       (make-elem "map"))
(def mark           (make-elem "mark"))
(def menu           (make-elem "menu"))
(def html-meta      (make-elem "meta"))
(def meter          (make-elem "meter"))
(def nav            (make-elem "nav"))
(def noframes       (make-elem "noframes"))
(def noscript       (make-elem "noscript"))
(def object         (make-elem "object"))
(def ol             (make-elem "ol"))
(def optgroup       (make-elem "optgroup"))
(def option         (make-elem "option"))
(def output         (make-elem "output"))
(def p              (make-elem "p"))
(def param          (make-elem "param"))
(def pre            (make-elem "pre"))
(def progress       (make-elem "progress"))
(def q              (make-elem "q"))
(def rp             (make-elem "rp"))
(def rt             (make-elem "rt"))
(def ruby           (make-elem "ruby"))
(def s              (make-elem "s"))
(def samp           (make-elem "samp"))
(def script         (make-elem "script"))
(def section        (make-elem "section"))
(def select         (make-elem "select"))
(def small          (make-elem "small"))
(def source         (make-elem "source"))
(def span           (make-elem "span"))
(def strike         (make-elem "strike"))
(def strong         (make-elem "strong"))
(def style          (make-elem "style"))
(def sub            (make-elem "sub"))
(def summary        (make-elem "summary"))
(def sup            (make-elem "sup"))
(def table          (make-elem "table"))
(def tbody          (make-elem "tbody"))
(def td             (make-elem "td"))
(def textarea       (make-elem "textarea"))
(def tfoot          (make-elem "tfoot"))
(def th             (make-elem "th"))
(def thead          (make-elem "thead"))
(def html-time      (make-elem "time"))
(def title          (make-elem "title"))
(def tr             (make-elem "tr"))
(def track          (make-elem "track"))
(def tt             (make-elem "tt"))
(def u              (make-elem "u"))
(def ul             (make-elem "ul"))
(def html-var       (make-elem "var"))
(def video          (make-elem "video"))
(def wbr            (make-elem "wbr"))

(def spliced        vector)
(def $text          #(.createTextNode js/document %))
(def $comment       #(.createComment js/document %))

(def *initfns* (atom []))

(defn add-initfn! [f]
  (swap! *initfns* into [f]))

(defn init [forms]
  (let [body (.-body js/document)]
    (gdom/removeChildren body)
    (when forms
      (doseq [x (unsplice forms)]
        (let [e (if (string? x) (.createTextNode js/document x) x)]
          (when (instance? js/Node e) (.appendChild body e))))) 
    (mapv (fn [f] (f)) @*initfns*)))

;; frp ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn timeout [f t] (.setTimeout js/window f t))

(->
  (js/jQuery "body")
  (.on "submit" (fn [event] (.preventDefault event))))

(defn rel
  [other event]
  (let [os (js->clj (.offset (js/jQuery other))) 
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
  (let [e (js/jQuery elem)]
    (case (.attr e "type")
      "checkbox" (apply check-val! e args)
      (apply text-val! e args))))

(defmethod do! :attr
  ([elem _ k]
   (.attr (js/jQuery elem) (name k)))
  ([elem _ k v & kvs]
   (js/jQuery
     #(let [e (js/jQuery elem)] 
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
     (js/jQuery #(.toggleClass (js/jQuery elem) c)))) 
  ([elem _ c switch & cswitches] 
   (js/jQuery
     (fn []
       (mapv (partial apply #(when-let [c (safe-name %1)]
                               (.toggleClass (js/jQuery elem) c (boolean %2)))) 
             (partition 2 (list* c switch cswitches)))))))

(defmethod do! :css
  ([elem _ k]
   (js/jQuery #(.css (js/jQuery elem) (safe-name k))))
  ([elem _ k v]
   (js/jQuery #(.css (js/jQuery elem) (safe-name k) v)))
  ([elem _ k v & more]
   (js/jQuery #(mapv (fn [[k v]] (do! elem :css k v))
                     (cons (list k v) (partition 2 more))))))

(defmethod do! :toggle
  [elem _ v]
  (js/jQuery #(.toggle (js/jQuery elem) (boolean v))))

(defmethod do! :slide-toggle
  [elem _ v]
  (js/jQuery
    #(if v
       (.slideDown (.hide (js/jQuery elem)) "fast")
       (.slideUp (js/jQuery elem) "fast"))))

(defmethod do! :fade-toggle
  [elem _ v]
  (js/jQuery
    #(if v
       (.fadeIn (.hide (js/jQuery elem)) "fast")
       (.fadeOut (js/jQuery elem) "fast"))))

(defmethod do! :focus
  [elem _ v]
  (js/jQuery #(if v (timeout (fn [] (.focus (js/jQuery elem))) 0)
                    (timeout (fn [] (.focusout (js/jQuery elem))) 0))))

(defmethod do! :select
  [elem _ _]
  (js/jQuery #(.select (js/jQuery elem))))

(defmethod do! :focus-select
  [elem _ v]
  (js/jQuery #(when v (do! elem :focus v) (do! elem :select v))))

(defmethod do! :text
  [elem _ v]
  (js/jQuery #(.text (js/jQuery elem) (str v))))

(defn on!
  [elem & event-callbacks]
  (let [elem (js/jQuery elem)]
    (doseq [[e f] (partition 2 event-callbacks)] 
      (js/jQuery (fn [] (.on elem (name e) f))))))

(defn fan-out
  "Given cell c containing a vector, returns a list of formula cells such that
  the nth formula cell contains the nth item in c."
  [c]
  (let [n (count @c)]
    (if (<= n 4)
      (mapv #(cell= (safe-nth c %)) (range 0 n))
      (let [p (.floor js/Math (/ n 2))
            u (cell= (subvec c 0 p))
            v (cell= (subvec c p))]
        (into (fan-out u) (fan-out v))))))

(defn mkpad [n x]
  (let [p (vec (repeat n x))]
    #(let [z (- n (count %))] (if (pos? z) (into % (subvec p 0 z)) p))))

(defn thing-looper [things n g & {:keys [reverse?]}] 
  (let [pad    (mkpad n nil)
        items  (fan-out (cell= (pad things)))]
    (fn [f container]
      (let [tpl #(apply f %1 (g things %1 %2))
            frg (.createDocumentFragment js/document)
            add #(timeout (fn [] (.appendChild frg (tpl %1 %2))) 0)]
        (if (not reverse?)
          (doall (map-indexed add items))
          (loop [i (dec (count items)) items items]
            (add i (peek items))
            (when-not (zero? i) (recur (dec i) (pop items))))) 
        (timeout #(.appendChild container frg) 0)
        container))))
