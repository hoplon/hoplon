(ns hoplon.goog
  (:require [goog.dom           :as dom]
            [goog.dom.classlist :as domcl]
            [goog.dom.forms     :as domf]
            [goog.events        :as events]
            [goog.fx.dom        :as fxdom]
            [goog.style         :as style]
            [hoplon.core        :refer [on! do! normalize-class]]))

;; Google Closure Library Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
