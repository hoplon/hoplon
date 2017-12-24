(ns hoplon.experimental
  (:require [clojure.string  :as string]))

(defmacro cache-key []
  (or (System/getProperty "hoplon.cacheKey")
      (let [u (.. (UUID/randomUUID) toString (replaceAll "-" ""))]
        (System/setProperty "hoplon.cacheKey" u)
        u)))

(defn bust-cache
  [path]
  (let [[f & more] (reverse (string/split path #"/"))
        [f1 f2]    (string/split f #"\." 2)]
    (->> [(str f1 "." (cache-key)) f2]
         (string/join ".")
         (conj more)
         (reverse)
         (string/join "/"))))

(defmacro definterval
  [sym timeout & body]
  `(def ~sym (do (when (~'exists? ~sym) (js/clearInterval ~sym))
                 (js/setInterval #(do ~@body) ~timeout))))

(defmacro elem+
  "Experimental."
  [[bind-attr bind-kids] & body]
  (let [attr-keys (map-bind-keys bind-attr)]
    `(fn [& args#]
       (let [[attr# kids#] (parse-args args#)]
         (-> (let [kids*# (j/cell [])
                   attr*# (j/cell ~(zipmap attr-keys (repeat nil)))]
               (j/cell-let [~bind-attr attr*#
                            ~bind-kids (j/cell= (flatten kids*#))]
                 (doto (do ~@body)
                   (set-appendChild! (constantly kids*#))
                   (set-removeChild! (constantly kids*#))
                   (set-setAttribute! (constantly attr*#)))))
             (apply attr# kids#))))))

(defmacro defelem+
  "Experimental.

  Defines an extended element function.

  An extended element function creates a DOM Element given two arguments:

    * `attrs` - a number of key-value pairs for attributes and their values
    * `kids` - a sequence of DOM Elements/Cells producing DOM Elements to be
      appended/used inside

  The returned DOM Element is itself a function which can accept more
  attributes and child elements:

    (defelem+ counter
      [{:keys [state class]} kids]
      (ul :class class, :click #(swap! state (fnil inc 0))
        (loop-tpl :bingings [kid kids]
          (li kid))

  Differences to `defelem`:

    - `kids` argument inside the `defelem+` is a Cell of DOM Elements
      representing any DOM elements supplied during construction or
      appended/removed at a later point in time.
    - `attrs` argument must be destructured as it's also a Cell."
  [name & forms]
  (let [[_ name [_ [bind & body]]] (macroexpand-1 `(defn ~name ~@forms))]
    `(def ~name (elem+ ~bind ~@body))))

(defmacro static
  "Experimental."
  [elem]
  `(let [id# ~(str (gensym "hl"))]
     (or (static-elements id#)
         (~elem :static-id id#))))

(defmacro sexp
  "Experimental."
  [& args]
  (->> (last (parse-e (cons '_ args)))
       (mapcat #(if-not (string? %)
                  [%]
                  (read-string (str "(" % "\n)"))))))
