(ns hoplon.dom
  (:require
    [clojure.set :as set]
    [hoplon.core :refer [do! normalize-class on! with-timeout]]))

(defmethod do! :value
  [elem _ v]
  (if (contains? #{"checkbox" "radio"} (.-type elem))
    (set! (.-checked elem) v) 
    (set! (.-value elem) v)))

(defmethod do! :class
  [elem _ kvs]
  (doseq [[c add?] (normalize-class kvs)]
    (if add?
      (.add (.-classList elem) (name c))
      (.remove (.-classList elem) (name c)))))

(defmethod do! :class/default
  [elem _ kvs]
  (do! elem :class kvs))

(defn- set-attributes!
  ([elem kvs]
   (doseq [[k v] kvs :let [k (name k)]]
     (if-not v
       (.removeAttribute elem k)
       (.setAttribute elem k (if (true? v) k v)))))
  ([elem k v & kvs]
   (set-attributes! elem (apply hash-map k v kvs))))

(defmethod do! :svg/default
  [elem key val]
  (set-attributes! elem key val))

(defmethod do! :smart-class
  [elem _ kvs]
  (if (map? kvs)
    (do! elem :class kvs)
    (let [new-class-list (if (string? kvs)
                           (.split kvs #"\s+")
                           (clj->js (mapv name kvs)))
          old-class-list (or (.-hoplon-smart-class ^js elem) #{})
          classes-to-remove (set/difference old-class-list (set new-class-list))]
      (set! (.-hoplon-smart-class ^js elem) (into old-class-list new-class-list))
      (.apply (.-add (.-classList elem)) (.-classList elem) new-class-list)
      (when-not (empty? classes-to-remove)
        (.apply (.-remove (.-classList elem)) (.-classList elem) (clj->js (vec classes-to-remove)))))))

(defmethod do! :toggle
  [elem _ v]
  (set! (.-display (.-style elem)) (if v "" "none")))

(defmethod do! :focus
  [elem _ v]
  (with-timeout 0
    (if v
      (.focus elem)
      (.blur elem))))

(defmethod do! :select
  [elem _ v]
  (if v
    (.select elem)
    (.removeAllRanges (.getSelection js/window))))

(defmethod do! :text
  [elem _ v]
  (set! (.-textContent elem) (str v)))

(defmethod do! :html
  [elem _ v]
  (.replaceChildren elem v))

(defmethod do! :dangerous-html
  [elem _ v]
  (if (string? v)
    (set! (.-innerHTML elem) v)
    (.replaceChildren elem v)))

(defmethod do! :scroll-to
  [elem _ v]
  (when v
    (.scrollIntoView elem #js {:behavior "smooth"})))

(extend-type js/Event
  cljs.core/IDeref
  (-deref [this]
    (.-value (.-target this))))

(defmethod on! :hoplon.core/default
  [elem event callback]
  (.addEventListener elem (name event) callback))
