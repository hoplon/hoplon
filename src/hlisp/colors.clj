(ns hlisp.colors
  (:refer-clojure :exclude [print println]))

(def ANSI-CODES
  {:reset           "[0m"
   :default         "[39m"
   :white           "[37m"
   :black           "[30m"
   :red             "[31m"
   :green           "[32m"
   :blue            "[34m"
   :yellow          "[33m"
   :magenta         "[35m"
   :cyan            "[36m"
   :bold-default    "[1;39m"
   :bold-white      "[1;37m"
   :bold-black      "[1;30m"
   :bold-red        "[1;31m"
   :bold-green      "[1;32m"
   :bold-blue       "[1;34m"
   :bold-yellow     "[1;33m"
   :bold-magenta    "[1;35m"
   :bold-cyan       "[1;36m"
   :under-default   "[4;39m"
   :under-white     "[4;37m"
   :under-black     "[4;30m"
   :under-red       "[4;31m"
   :under-green     "[4;32m"
   :under-blue      "[4;34m"
   :under-yellow    "[4;33m"
   :under-magenta   "[4;35m"
   :under-cyan      "[4;36m"
   })

(defn ansi
  [code]
  (str \u001b (get ANSI-CODES code (:reset ANSI-CODES))))

(defn style
  [s & codes]
  (str (apply str (map ansi codes)) s (ansi :reset)))

(defn pr-ok [ok? text]
  (clojure.core/print (style text (if ok? :green :red))))

(defn print [& args]
  (apply clojure.core/print args)
  (flush))

(defn println [& args]
  (apply clojure.core/println args)
  (flush))

