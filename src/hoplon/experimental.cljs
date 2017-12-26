(ns hoplon.experimental
  (:include-macros [hoplon.experimental :refer [cache-key]]))

(def ^:no-doc static-elements
  "This is an internal implementation detail, exposed for the convenience of
  the hoplon.core/static macro. Experimental."
  (-> #(assoc %1 (.getAttribute %2 "static-id") %2)
      (reduce {} (.querySelector js/document "[static-id]"))))

(defn bust-cache
  "Public helper.
  Experimental."
  [path]
  (let [[f & more] (reverse (split path #"/"))
        [f1 f2]    (split f #"\." 2)]
    (->> [(str f1 "." (cache-key)) f2]
         (join ".")
         (conj more)
         (reverse)
         (join "/"))))
