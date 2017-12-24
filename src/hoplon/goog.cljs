(ns hoplon.goog
  (:require [goog.dom :as dom]
            [goog.dom.classlist :as domcl]
            [goog.dom.forms :as domf]
            [goog.events :as events]
            [goog.fx.dom :as fxdom]
            [goog.style :as style]
            [goog.object :as obj]
            [hoplon.core :refer [on! do! normalize-class]]
            [hoplon.spec :as spec])
  (:require-macros
    [javelin.core   :refer [with-let cell= prop-cell]]
    [hoplon.core    :refer [with-timeout with-dom]]))

;; Google Closure Library Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do! :value ;; only for elements with a 'type' attribute (inputs).
  [elem _ v]
  (domf/setValue elem v))

(defmethod spec/do! :value
  [_]
  (spec/attr any?))

(defmethod do! :class
  [elem _ kvs]
  (doseq [[c p?] (normalize-class kvs)]
    (domcl/enable elem (name c) (boolean p?))))

(defmethod spec/do! :class
  [_]
  (spec/attr :hoplon.spec/class))

(defmethod do! :toggle
  [elem _ v]
  (style/setElementShown elem (boolean v)))

(defmethod spec/do! :toggle
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :slide-toggle
  [elem _ v]
  (comp
    (if v
      (fxdom/swipe elem [0 0] (style/getSize elem))
      (fxdom/swipe elem (style/getSize elem) [0 0]))
    (style/setElementShown elem (boolean v))))

(defmethod spec/do! :slide-toggle
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :fade-toggle
  [elem _ v]
  (if v
    (fxdom/fadeInAndShow elem 200)
    (fxdom/fadeOutAndHide elem 200)))

(defmethod spec/do! :fade-toggle
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :focus
  [elem _ v]
  (with-timeout 0
    (if v
      (events/dispatchEvent elem goog.events.EventType.FOCUS)
      (events/dispatchEvent elem goog.events.EventType.FOCUSOUT))))

(defmethod spec/do! :focus
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :select
  [elem _ _]
  (events/dispatchEvent elem goog.events.EventType.SELECT))

(defmethod spec/do! :select
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :focus-select
  [elem _ v]
  (when v (do! elem :focus v) (do! elem :select v)))

(defmethod spec/do! :focus-select
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :text
  [elem _ v]
  (dom/setTextContent elem (str v)))

(defmethod spec/do! :text
  [_]
  (spec/attr :hoplon.spec/string))

(defmethod do! :html
  [elem _ v]
  (comp
    (dom/removeChildren elem)
    (dom/appendChild elem (dom/safeHtmlToNode v))))

(defmethod spec/do! :html
  [_]
  (spec/attr :hoplon.spec/string))

(defmethod do! :scroll-to
  [elem _ v]
  (when v
    (style/scrollContinerIntoView elem (dom/getDocument))))

(defmethod spec/do! :scroll-to
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod on! :hoplon.core/default
  [elem event callback]
  (let [event (obj/get events/EventType (name event))]
    (when-dom elem #(events/listen elem event callback))))
