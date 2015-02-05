;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.hoplon.boot
  {:boot/export-tasks true}
  (:require [boot.core :as boot]
            [clojure.pprint :as pp]
            [tailrecursion.hoplon.compiler.compiler :as hl]))

(boot/deftask hoplon
  "Build Hoplon web application.

  This task accepts an optional map of options to pass to the Hoplon compiler.
  Further ClojureScript compilation rely on another task (e. g. boot-cljs).
  The Hoplon compiler recognizes the following options:

  * :pretty-print  If set to `true` enables pretty-printed output
                   in the ClojureScript files created by the Hoplon compiler.

  If you are compiling library, you need to include resulting cljs in target.
  Do it by specifying :lib flag."
  [pp pretty-print bool "Pretty-print CLJS files created by the Hoplon compiler."
   l  lib          bool "Include produced cljs in the final artefact."]
  (let [tmp-cljs (boot/temp-dir!)
        tmp-html (boot/temp-dir!)
        prev-fileset (atom nil)
        opts (dissoc *opts* :lib)
        add-cljs (if lib boot/add-resource boot/add-source)]
    (boot/with-pre-wrap fileset
      (println "Compiling Hoplon pages...")
      (boot/empty-dir! tmp-html)
      (let [hl (->> fileset
                    (boot/fileset-diff @prev-fileset)
                    boot/input-files
                    (boot/by-ext [".hl"])
                    (map boot/tmpfile))]
        (reset! prev-fileset fileset)
        (doseq [f hl]
          (println "•" (.getPath f))
          (hl/compile-file f tmp-cljs tmp-html :opts opts)))
      (-> fileset
          (add-cljs tmp-cljs)
          (boot/add-resource tmp-html)
          boot/commit!))))

(boot/deftask html2cljs
  "Convert file from html syntax to cljs syntax."
  [f file FILENAME str "File to convert."]
  (boot/with-pre-wrap fileset
    (->> file str slurp hl/as-forms
         (#(with-out-str (pp/write % :dispatch pp/code-dispatch)))
         clojure.string/trim
         (#(subs % 1 (dec (count %))))
         print)
    fileset))