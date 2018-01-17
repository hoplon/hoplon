(ns hoplon.test-util
 (:refer-clojure :exclude [find]))

(defn find
 [el sel]
 (array-seq
  (.querySelectorAll el sel)))

(defn trigger!
 [el name]
 (let [e (.createEvent js/document "UIEvents")]
  (.initEvent e name true true)
  (.dispatchEvent el e)))

(defn matches
 [el sel]
 (.webkitMatchesSelector el sel))

(defn text
 [el]
 (.-textContent el))
