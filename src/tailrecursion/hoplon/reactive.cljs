(ns tailrecursion.hoplon.reactive
  (:require [tailrecursion.hoplon.env :as h :refer [clone]]))

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
  (if (satisfies? tailrecursion.hoplon.env/IDomNode elem)
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

(defn- safe-name [x]
  (try (name x) (catch js/Error e)))

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

(defn- delegate
  [atm event]
  (.on (js/jQuery js/document) event "[data-hl]" #(reset! atm %))
  atm)

(def events {
  :change       (delegate (atom nil) "change")
  :click        (delegate (atom nil) "click")
  :dblclick     (delegate (atom nil) "dblclick")
  :error        (delegate (atom nil) "error")
  :focus        (delegate (atom nil) "focus")
  :focusin      (delegate (atom nil) "focusin")
  :focusout     (delegate (atom nil) "focusout")
  :hover        (delegate (atom nil) "hover")
  :keydown      (delegate (atom nil) "keydown")
  :keypress     (delegate (atom nil) "keypress")
  :keyup        (delegate (atom nil) "keyup")
  :load         (delegate (atom nil) "load")
  :mousedown    (delegate (atom nil) "mousedown")
  :mouseenter   (delegate (atom nil) "mouseenter")
  :mouseleave   (delegate (atom nil) "mouseleave")
  :mousemove    (delegate (atom nil) "mousemove")
  :mouseout     (delegate (atom nil) "mouseout")
  :mouseover    (delegate (atom nil) "mouseover")
  :mouseup      (delegate (atom nil) "mouseup")
  :ready        (delegate (atom nil) "ready")
  :scroll       (delegate (atom nil) "scroll")
  :select       (delegate (atom nil) "select")
  :submit       (delegate (atom nil) "submit")
  :unload       (delegate (atom nil) "unload")})

(defn- do-on!
  [elem event callback]
  (let [event   (get events (keyword event))
        update  #(if (and (not= %3 %4) ((filter-id (id elem)) %4)) (callback %4))]
    (add-watch event (gensym) update)))

(defn on!
  [elem & event-callbacks]
  (mapv (partial apply do-on! elem) (partition 2 event-callbacks)))

(defn thing-looper [things g]
  (fn [f container]
    (into container (mapv #(apply f % (g things %))
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
