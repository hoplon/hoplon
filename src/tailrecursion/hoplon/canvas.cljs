(ns tailrecursion.hoplon.canvas
  (:require-macros
    [tailrecursion.javelin.macros :refer [cell cell=]])
  (:require
    [clojure.walk                   :as walk]
    [tailrecursion.javelin          :as j]
    [tailrecursion.hoplon.env       :as h]
    [tailrecursion.hoplon.reactive  :as r]))

(defn ctx-config!
  [ctx opts]
  (if (:font            opts) (set! (.-font           ctx) (:font             opts))) 
  (if (:alpha           opts) (set! (.-globalAlpha    ctx) (:alpha            opts))) 
  (if (:fill-style      opts) (set! (.-fillStyle      ctx) (:fill-style       opts))) 
  (if (:stroke-style    opts) (set! (.-strokeStyle    ctx) (:stroke-style     opts))) 
  (if (:shadow-color    opts) (set! (.-shadowColor    ctx) (:shadow-color     opts))) 
  (if (:shadow-blur     opts) (set! (.-shadowBlur     ctx) (:shadow-blur      opts))) 
  (if (:shadow-offset-x opts) (set! (.-shadowOffsetX  ctx) (:shadow-offset-x  opts))) 
  (if (:shadow-offset-y opts) (set! (.-shadowOffsetY  ctx) (:shadow-offset-y  opts)))
  (if (:line-width      opts) (set! (.-lineWidth      ctx) (:line-width       opts))))

(declare ctx-do!)

(defn ctx-do-1!
  [ctx op]
  (case (first op)
    :do           (vec (rest op))
    :with         `[[:save] [:set ~(nth op 1)] ~@(drop 2 op) [:restore]]
    :clear        (let [c (.-canvas ctx)] (.clearRect ctx 0 0 (.-width c) (.-height c)))
    :fill         (let [c (.-canvas ctx)] (.fillRect ctx 0 0 (.-width c) (.-height c)))
    :set          (ctx-config!  ctx (nth op 1))
    :save         (.save        ctx)
    :restore      (.restore     ctx)
    :clear-rect   (.clearRect   ctx (nth op 1) (nth op 2) (nth op 3) (nth op 4))
    :stroke-rect  (.strokeRect  ctx (nth op 1) (nth op 2) (nth op 3) (nth op 4))
    :fill-rect    (.fillRect    ctx (nth op 1) (nth op 2) (nth op 3) (nth op 4))
    :stroke-text  (.strokeText  ctx (nth op 1) (nth op 2) (nth op 3))
    :fill-text    (.fillText    ctx (nth op 1) (nth op 2) (nth op 3))
    :draw-image   (.drawImage   ctx (nth op 1) (nth op 2) (nth op 3))
    :sleep        (fn [x] (js/setTimeout #(ctx-do! ctx x) (nth op 1))) 
    nil))

(defn ctx-do!
  [ctx ops]
  (trampoline
    (fn iter [ops]
      (when-first [o ops]
        (let [f (ctx-do-1! ctx o)]
          (cond (fn? f)     (f (rest ops))
                (vector? f) #(iter (into f (rest ops)))
                :else       #(iter (rest ops))))))
    ops))

(defn canvas-do!
  [c ops]
  (if c (ctx-do! (.getContext c "2d") ops)))

(defn make-canvas
  [canvas state & handlers]
  (let [canvas  (h/clone canvas)
        add-hlr (fn [[e h]] (r/on! canvas e (r/rel-event canvas "canvas" h)))]
    (h/add-initfn!
      (fn []
        (cell= (canvas-do! (aget (r/dom-get canvas) 0) state)) 
        (mapv add-hlr (partition 2 handlers)))) 
    canvas))

(defn draw-things
  [things tpl]
  (walk/postwalk #(if (fn? %) `[:do ~@(map-indexed % things)] %) tpl))
