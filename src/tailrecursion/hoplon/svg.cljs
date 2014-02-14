(ns tailrecursion.hoplon.svg
  (:refer-clojure :exclude [symbol]))

(defn make-svg-ctor [tag]
  (let [xmlns "http://www.w3.org/2000/svg"]
    (fn [& args]
      (apply (.createElementNS js/document xmlns tag) args))))

(def svg      (make-svg-ctor       "svg"))
(def g        (make-svg-ctor         "g"))
(def rect     (make-svg-ctor      "rect"))
(def circle   (make-svg-ctor    "circle"))
(def ellipse  (make-svg-ctor   "ellipse"))
(def line     (make-svg-ctor      "line"))
(def polyline (make-svg-ctor  "polyline"))
(def polygon  (make-svg-ctor   "polygon"))
(def path     (make-svg-ctor      "path"))
(def marker   (make-svg-ctor    "marker"))
(def text     (make-svg-ctor      "text"))
(def tspan    (make-svg-ctor     "tspan"))
(def tref     (make-svg-ctor      "tref"))
(def textpath (make-svg-ctor  "textpath"))
(def switch   (make-svg-ctor    "switch"))
(def image    (make-svg-ctor     "image"))
(def a        (make-svg-ctor     "svg:a"))
(def defs     (make-svg-ctor      "defs"))
(def symbol   (make-svg-ctor    "symbol"))
(def use      (make-svg-ctor       "use"))
(def title    (make-svg-ctor "svg:title"))
