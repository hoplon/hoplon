(ns hoplon.jquery
  (:require [hoplon.core :refer [do! on! when-dom normalize-class]]
            [cljsjs.jquery]
            [hoplon.spec :as spec])
  (:require-macros
    [javelin.core   :refer [with-let cell= prop-cell]]
    [hoplon.core    :refer [with-timeout]]))

;; Helper Fn's ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-attributes!
  ([this kvs]
   (let [e (js/jQuery this)]
     (doseq [[k v] kvs :let [k (name k)]]
       (if-not v
         (.removeAttr e k)
         (.attr e k (if (true? v) k v))))))
  ([this k v & kvs]
   (set-attributes! this (apply hash-map k v kvs))))

(defn set-styles!
  ([this kvs]
   (let [e (js/jQuery this)]
     (doseq [[k v] kvs]
       (.css e (name k) (str v)))))
  ([this k v & kvs]
   (set-styles! this (apply hash-map k v kvs))))

(defn text-val!
  ([e] (.val e))
  ([e v] (let [v (str v)]
           (when (not= v (text-val! e))
             (.val e v)))))

(defn check-val!
  ([e] (.is e ":checked"))
  ([e v] (.prop e "checked" (boolean v))))

;; jQuery Attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod do! :hoplon.core/default
  [elem key val]
  (do! elem :attr {key val}))

(defmethod spec/do! :hoplon.core/default
  [_]
  (spec/attr any?))

(defmethod do! :css/*
  [elem key val]
  (set-styles! elem key val))

(defmethod spec/do! :data/*
  [_]
  (spec/attr any?))

(defmethod do! :html/*
  [elem key val]
  (set-attributes! elem key val))

(defmethod spec/do! :html/*
  [_]
  (spec/attr any?))

(defmethod do! :svg/*
  [elem key val]
  (set-attributes! elem key val))

(defmethod spec/do! :svg/*
  [_]
  (spec/attr any?))

(defmethod do! :attr/*
  [elem _ kvs]
  (set-attributes! elem kvs))

(defmethod spec/do! :data/*
  [_]
  (spec/attr :hoplon.spec/map))

(defmethod do! :prop/*
  [elem key val]
  (let [e (js/jQuery elem)]
    (.prop e (name key) val)))

(defmethod spec/do! :prop/*
  [_]
  (spec/attr any?))

(defmethod do! :data/*
  [elem key val]
  (let [e (js/jQuery elem)]
    (.data e (name key) val)))

(defmethod spec/do! :data/*
  [_]
  (spec/attr any?))

(defmethod do! :attr
  [elem _ kvs]
  (set-attributes! elem kvs))

(defmethod spec/do! :attr
  [_]
  (spec/attr :hoplon.spec/map))

(defmethod do! :css
  [elem _ kvs]
  (set-styles! elem kvs))

(defmethod spec/do! :css
  [_]
  (spec/attr :hoplon.spec/map))

(defmethod do! :value
  [elem _ & args]
  (let [e (js/jQuery elem)]
    (apply (if (= "checkbox" (.attr e "type")) check-val! text-val!) e args)))

(defmethod spec/do! :value
  [_]
  (spec/attr :hoplon.spec/value))

(defmethod do! :class
  [elem _ kvs]
  (let [elem  (js/jQuery elem)
        clmap (normalize-class kvs)]
    (doseq [[c p?] clmap] (.toggleClass elem (name c) (boolean p?)))))

(defmethod spec/do! :class
  [_]
  (spec/attr :hoplon.spec/class))

(defmethod do! :class/*
  [elem _ kvs]
  (do! elem :class kvs))

(defmethod spec/do! :class/*
  [_]
  (spec/attr :hoplon.spec/class))

(defmethod do! :toggle
  [elem _ v]
  (.toggle (js/jQuery elem) (boolean v)))

(defmethod spec/do! :toggle
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :slide-toggle
  [elem _ v]
  (if v
    (.slideDown (.hide (js/jQuery elem)) "fast")
    (.slideUp (js/jQuery elem) "fast")))

(defmethod spec/do! :slide-toggle
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :fade-toggle
  [elem _ v]
  (when-dom elem
    #(if v
       (.fadeIn (.hide (js/jQuery elem)) "fast")
       (.fadeOut (js/jQuery elem) "fast"))))

(defmethod spec/do! :fade-toggle
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :focus
  [elem _ v]
  (with-timeout 0
    (if v (.focus (js/jQuery elem)) (.focusout (js/jQuery elem)))))

(defmethod spec/do! :focus
  [_]
  (spec/attr :hoplon.spec/boolean))

(defmethod do! :select
  [elem _ _]
  (.select (js/jQuery elem)))

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
  (.text (js/jQuery elem) (str v)))

(defmethod spec/do! :text
  [_]
  (spec/attr :hoplon.spec/string))

(defmethod do! :html
  [elem _ v]
  (.html (js/jQuery elem) v))

(defmethod spec/do! :html
  [_]
  (spec/attr :hoplon.spec/string))

(defmethod do! :scroll-to
  [elem _ v]
  (when v
    (let [body (js/jQuery "body,html")
          elem (js/jQuery elem)]
      (.animate body (clj->js {:scrollTop (.-top (.offset elem))})))))

(defmethod spec/do! :scroll-to
  [_]
  (spec/attr :hoplon.spec/boolean))

(extend-type js/jQuery.Event
  cljs.core/IDeref
  (-deref [this] (-> this .-target js/jQuery .val)))

(defmethod on! :hoplon.core/default
  [elem event callback]
  (when-dom elem #(.on (js/jQuery elem) (name event) callback)))

(defmethod on! :html/*
  [elem event callback]
  (when-dom elem #(.on (js/jQuery elem) (name event) callback)))
