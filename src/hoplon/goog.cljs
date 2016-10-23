(ns hoplon.goog
  (:require [goog.dom :as dom]
            [goog.dom.classlist :as domcl]
            [goog.dom.forms :as domf]
            [goog.events :as events]
            [goog.fx.dom :as fxdom]
            [goog.style :as style]
            [hoplon.core :refer [on! do! normalize-class]])
  (:require-macros
    [javelin.core   :refer [with-let cell= prop-cell]]
    [hoplon.core    :refer [cache-key with-timeout with-dom]]))

;; Google Closure Library Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do! :value ;; only for elements with a 'type' attribute (inputs).
  [elem _ v]
  (domf/setValue elem v))

(defmethod do! :class
  [elem _ kvs]
  (doseq [[c p?] (normalize-class kvs)]
    (domcl/enable elem (name c) (boolean p?))))

(defmethod do! :toggle
  [elem _ v]
  (style/setElementShown elem (boolean v)))

(defmethod do! :slide-toggle
  [elem _ v]
  (comp
    (if v
      (fxdom/swipe elem [0 0] (style/getSize elem))
      (fxdom/swipe elem (style/getSize elem) [0 0]))
    (style/setElementShown elem (boolean v))))

(defmethod do! :fade-toggle
  [elem _ v]
  (if v
    (fxdom/fadeInAndShow elem 200)
    (fxdom/fadeOutAndHide elem 200)))

(defmethod do! :focus
  [elem _ v]
  (with-timeout 0
    (if v
      (events/dispatchEvent elem goog.events.EventType.FOCUS)
      (events/dispatchEvent elem goog.events.EventType.FOCUSOUT))))

(defmethod do! :select
  [elem _ _]
  (events/dispatchEvent elem goog.events.EventType.SELECT))

(defmethod do! :focus-select
  [elem _ v]
  (when v (do! elem :focus v) (do! elem :select v)))

(defmethod do! :text
  [elem _ v]
  (dom/setTextContent elem (str v)))

(defmethod do! :html
  [elem _ v]
  (comp
    (dom/removeChildren elem)
    (dom/appendChild elem (dom/safeHtmlToNode v))))

(defmethod do! :scroll-to
  [elem _ v]
  (when v
    (style/scrollContinerIntoView elem (dom/getDocument))))
