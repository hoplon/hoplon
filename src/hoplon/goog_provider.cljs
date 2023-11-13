(ns hoplon.goog_provider
  (:require
   [applied-science.js-interop :as j]
   [goog.dom           :as dom]
   [goog.dom.classlist :as domcl]
   [goog.dom.forms     :as domf]
   [goog.events        :as events]
   [goog.fx.dom        :as fxdom]
   [goog.style         :as style]
   [hoplon.core        :refer [do! normalize-class on!]]))

(defn- set-attributes!
  ([elem kvs]
   (doseq [[k v] kvs :let [k (name k)]]
     (if-not v
       (j/call elem :removeAttribute k)
       (j/call elem :setAttribute k (if (true? v) k v)))))
  ([elem k v & kvs]
   (set-attributes! elem (apply hash-map k v kvs))))

;; Google Closure Library Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do! :svg/default
  [elem key val]
  (set-attributes! elem key val))

(defmethod do! :value
  [elem _ v]
  (domf/setValue elem v))

(defmethod do! :class
  [elem _ kvs]
  (doseq [[c p?] (normalize-class kvs)]
    (domcl/enable elem (name c) (boolean p?))))

(defmethod do! :class/default
  [elem _ kvs]
  (do! elem :class kvs))

(defmethod do! :toggle
  [elem _ v]
  (style/setElementShown elem (boolean v)))

(defmethod do! :fade-toggle
  [elem _ v]
  (if v
    (.play (fxdom/FadeInAndShow. elem 200))
    (.play (fxdom/FadeOutAndHide. elem 200))))

(defmethod do! :text
  [elem _ v]
  (dom/setTextContent elem (str v)))

(extend-type goog.events.BrowserEvent
  cljs.core/IDeref
  (-deref [this]
    (-> this .-target .-value)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Google Closure Library Events ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod on! :hoplon.core/default
  [elem event callback]
  (events/listen elem (name event) callback))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
