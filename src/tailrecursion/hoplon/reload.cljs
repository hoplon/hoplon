(ns tailrecursion.hoplon.reload
  (:require
   [clojure.string :as string]
   [tailrecursion.hoplon :as h])
  (:require-macros
   [tailrecursion.hoplon :refer [with-init!]]))

(defn- reload! [sheet]
  (when-let [h (.-href sheet)]
    (let [k "___tailrecursion_hoplon_reload___="
          q (str k (.getTime (js/Date.)))]
      (set! (.. sheet -ownerNode -href)
        (cond
          (< (.indexOf h "?") 0) (str h "?" q)
          (< (.indexOf h   k) 0) (str h "&" q)
          :else   (string/replace h (re-pattern (str k "\\d+")) q))))))

(defn file-modified-atom
  "Returns an atom whose value reflects the last modified time of the resource
  at the given `url`. The last modified time is obtained by polling the server
  with the given `interval` (in msec) with `HEAD` requests. If the response does
  not contain the special `X-Dev-Mode` header with the value `true` polling is 
  then disabled."
  [url interval]
  (let [last-mod (atom nil)
        xhr-opts {:url url :type "HEAD" :dataType "text"}
        xhr-dev? #(= "true" (.getResponseHeader % "X-Dev-Mode"))
        do-ajax  #(.ajax js/jQuery (clj->js (assoc %1 :success %2)))
        xhr-mod  #(.parse js/Date (.getResponseHeader % "Last-Modified"))]
    ((fn do-poll [& [_ _ xhr]]
       (when xhr (reset! last-mod (xhr-mod xhr)))
       (when (or (not xhr) (xhr-dev? xhr))
         (js/setTimeout #(do-ajax xhr-opts do-poll) interval))))
    last-mod))

(defn on-modified
  "Runs the given `callback` whenever the last modified time of the resource at
  the given `url` changes, polling with the given `interval` in milliseconds."
  [url interval callback]
  (add-watch (file-modified-atom url (or interval 100)) nil
    #(when (and %3 (not= %3 %4)) (callback))))

(defn reload-js
  "Reloads the page whenever the `main.js` file is modified. The optional
  `interval` argument specifies how often to poll the server for changes, in
  milliseconds."
  [& [interval]]
  (on-modified "main.js" interval #(.. js/window -location reload)))

(defn reload-css
  "Reloads CSS stylesheets whenever they are modified. The page itself is not
  reloaded, just the stylesheets. The optional `interval` argument specifies
  how often to poll the server for changes, in milliseconds."
  [& [interval]]
  ((fn wait-css []
     (let [css     (.. js/document -styleSheets)
           each    (range 0 (.-length css))
           css-seq (keep #(.-href %) (for [s each] (aget css s)))]
       (if-not (seq css-seq)
         (js/setTimeout wait-css (or interval 100))
         (doseq [s (range 0 (.-length css))]
           (when-let [sheet (aget css s)]
             (when-let [href (.-href sheet)]
               (on-modified href interval #(reload! (aget css s)))))))))))

(defn reload-all
  "Reload the page when `main.js` is modified, and CSS stylesheets as needed.
  The optional `interval` argument specifies how often to poll the server for
  changes, in milliseconds."
  [& [interval]]
  (reload-js interval)
  (reload-css interval))
