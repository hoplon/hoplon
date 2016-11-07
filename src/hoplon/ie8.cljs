(ns hoplon.ie8
  (:require [hoplon.core :refer [mk! add-attributes! remove-all-kids! add-children!]]
            [hoplon.protocol :refer [IHoplonElement]]))

;; Private Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private is-ie8 (not (obj/get js/window "Node")))

(def ^:private vector?*
  (if-not is-ie8
    vector?
    #(try (vector? %) (catch js/Error _))))

(def ^:private seq?*
  (if-not is-ie8
    seq?
    #(try (seq? %) (catch js/Error _))))

(defn- parse-args
  [args]
  (loop [attr (transient {})
         kids (transient [])
         [arg & args] args]
    (if-not arg
      [(persistent! attr) (persistent! kids)]
      (cond (map? arg)       (recur (reduce-kv #(assoc! %1 %2 %3) attr arg) kids args)
            (attribute? arg) (recur (assoc! attr arg (first args)) kids (rest args))
            (seq?* arg)      (recur attr (reduce conj! kids (flatten arg)) args)
            (vector?* arg)   (recur attr (reduce conj! kids (flatten arg)) args)
            :else            (recur attr (conj! kids arg) args)))))

;; Hoplon IE8 Constructors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod mk! :elem
  [elem _]
  (fn [& args]
    (let [[attrs kids] (parse-args args)]
      (add-attributes! elem attrs)
      (when (not (:static attrs))
        (remove-all-kids! elem)
        (add-children! elem kids)))))

(defmethod mk! :tag
  [tag _]
  #(-> js/document (.createElement tag) ensure-kids! (apply %&)))

(defmethod mk! :html
  [elem _]
  (fn [& args]
    (add-attributes! elem (nth (parse-args args) 0))))

(defmethod mk! :head
  [elem _]
  (mk! (.. elem -documentElement -firstChild) :elem))

;; Hoplon IE8 Element ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type js/Element
  IHoplonElement
  (-append-child!
    ([this child]
     (try (.appendChild this child) (catch js/Error _)))))
