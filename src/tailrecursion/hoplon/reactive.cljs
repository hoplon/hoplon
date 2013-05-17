(ns tailrecursion.hoplon.reactive
  (:require-macros
    [tailrecursion.javelin.macros :refer [cell]])
  (:require
    [tailrecursion.javelin        :as j]
    [tailrecursion.hoplon.env     :as hl]
    [tailrecursion.hoplon.util    :as hu]))

(declare ids)

(defn timeout [f t] (.setTimeout js/window f t))

(defn id      [e] (peek (ids e)))
(defn ids     [e] (.-ids e))
(defn id!     [e] (if-not (seq (ids e)) (hl/clone e) e))
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

(defn filter-not-disabled
  [v]
  (->
    (js/jQuery (.-target v))
    (.is "[data-disabled]")
    not))

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

(defn value!
  [elem & args] 
  (let [e (dom-get elem)]
    (case (.attr e "type")
      "checkbox" (apply check-val! e args)
      (apply text-val! e args))))

(defn- safe-name [x]
  (try (name x) (catch js/Error e)))

(defn attr!
  ([elem k]
   (.attr (dom-get elem) (name k)))
  ([elem k v & kvs]
   (js/jQuery
     #(let [e (dom-get elem)] 
        (mapv (fn [[k v]]
                (when-let [k (safe-name k)]
                  (case v
                    true   (.attr e k k)
                    false  (.removeAttr e k)
                    (.attr e k (str v)))))
              (partition 2 (list* k v kvs)))))))

(defn class!
  ([elem c] 
   (when-let [c (safe-name c)]
     (js/jQuery #(.toggleClass (dom-get elem) c)))) 
  ([elem c switch & cswitches] 
   (js/jQuery
     (fn []
       (mapv (partial apply #(when-let [c (safe-name %1)]
                               (.toggleClass (dom-get elem) c (boolean %2)))) 
             (partition 2 (list* c switch cswitches)))))))

(defn css! 
  ([elem k]
   (js/jQuery #(.css (dom-get elem) (safe-name k))))
  ([elem k v]
   (js/jQuery #(.css (dom-get elem) (safe-name k) v)))
  ([elem k v & more]
   (js/jQuery #(mapv (fn [[k v]] (css! elem k v))
                     (cons (list k v) (partition 2 more))))))

(defn toggle!
  [elem v]
  (js/jQuery #(.toggle (dom-get elem) (boolean v))))

(defn slide-toggle!
  [elem v]
  (js/jQuery
    #(if v
       (.slideDown (.hide (dom-get elem)) "fast")
       (.slideUp (dom-get elem) "fast"))))

(defn fade-toggle!
  [elem v]
  (js/jQuery
    #(if v
       (.fadeIn (.hide (dom-get elem)) "fast")
       (.fadeOut (dom-get elem) "fast"))))

(defn focus!
  [elem v]
  (js/jQuery #(if v (timeout (fn [] (.focus (dom-get elem))) 0)
                    (timeout (fn [] (.focusout (dom-get elem))) 0))))

(defn select!
  [elem _]
  (js/jQuery #(.select (dom-get elem))))

(defn text!
  [elem v]
  (js/jQuery #(.text (dom-get elem) (str v))))

(defn disabled?
  [elem]
  (.is (dom-get elem) "[data-disabled]"))

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
  (let [c       (cell nil)
        event   (get events (keyword event))
        update  #(if (and (not= %3 %4) ((filter-id (id elem)) %4)) (callback %4))]
    (add-watch event (gensym) update)))

(defn on!
  [elem & event-callbacks]
  (mapv (partial apply do-on! elem) (partition 2 event-callbacks)))

(defn thing-looper [things g]
  (fn [f container]
    (into container (mapv #(apply f % (g things %))
                          (range 0 (count @things))))))
