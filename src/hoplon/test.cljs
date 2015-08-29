(ns hoplon.test
  (:require
    [cljs.test      :as t]
    [goog.string    :as gstring]
    [clojure.string :as string]
    [hoplon.core    :as h :include-macros true]
    [javelin.core   :as j :include-macros true])
  (:import
    [goog.string StringBuffer]))

(defonce results
  (j/with-let [ret (j/cell nil)]
    (let [sb (goog.string.StringBuffer.)]
      (set! cljs.core/*print-newline* true)
      (set! cljs.core/*print-fn* (fn [x] (.append sb x)))
      (defmethod t/report [:cljs.test/default :end-run-tests] [m]
        (reset! ret (merge m {:pass? (t/successful? m) :output (str sb)}))))))

(def pass-icon "http://s15.postimg.org/7mvosn3rf/green_smiley_face_md.png")
(def fail-icon "http://s24.postimg.org/bkibcfnzp/red_smiley_face_md.png")

(j/defc= pass?      (:pass? results))
(j/defc= note-title (case pass? true "PASS"  false "FAIL" nil "----"))
(j/defc= note-body  (string/join " / " ((juxt :pass :fail :error) results)))
(j/defc= note-icon  (case pass? nil nil true pass-icon false fail-icon))
(j/defc= page-title (str note-title ": " note-body))

(defn notify [timeout title body icon]
  (try (when (= "default" (.-permission js/Notification))
         (.requestPermission js/Notification))
       (let [n (js/Notification. title (js-obj "icon" icon "body" body))]
         (when timeout (h/with-timeout timeout (.close n))))
       (catch js/Error _)))

(defn enable-notifications! []
  (j/cell= (when-not (nil? results) (notify 5000 note-title note-body note-icon))))

(h/on-page-load #(.. js/location reload))

(defn enable-page-output! []
  (h/html (h/head (h/title :text page-title))
          (h/body (h/div :css {:width "800px" :margin "0 auto"}
                         (h/pre (h/samp :text (j/cell= (:output results))))))))
