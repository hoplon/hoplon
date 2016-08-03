(ns hoplon.jquery
  (:require [hoplon.core :refer [do!]]
            [cljsjs.jquery])
  (:require-macros
    [javelin.core   :refer [with-let cell= prop-cell]]
    [hoplon.core    :refer [cache-key with-timeout with-dom]]))

;; frp helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn text-val!
  ([e] (.val e))
  ([e v] (let [v (str v)]
           (when (not= v (text-val! e))
             (.val e v)))))

(defn check-val!
  ([e] (.is e ":checked"))
  ([e v] (.prop e "checked" (boolean v))))

;; jQuery Attributes
(defmethod do! :value
  [elem _ & args]
  (let [e (js/jQuery elem)]
    (apply (if (= "checkbox" (.attr e "type")) check-val! text-val!) e args)))

(defmethod do! :class
  [elem _ kvs]
  (let [elem  (js/jQuery elem)
        ->map #(zipmap % (repeat true))
        clmap (if (map? kvs)
                kvs
                (->map (if (string? kvs) (.split kvs #"\s+") (seq kvs))))]
    (doseq [[c p?] clmap] (.toggleClass elem (name c) (boolean p?)))))

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

(extend-type js/jQuery.Event
  cljs.core/IDeref
  (-deref [this] (-> this .-target js/jQuery .val)))
