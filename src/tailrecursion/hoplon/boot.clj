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
            [tailrecursion.hoplon.compiler.compiler :as hl]))

(boot/deftask hoplon
  "Build Hoplon web application.

  This task accepts an optional map of options to pass to the Hoplon compiler.
  Further ClojureScript compilation rely on another task (e. g. boot-cljs).
  The Hoplon compiler recognizes the following options:

  * :cache         If set to `false` in-memory caching of compiled output is
                   disabled.

  * :pretty-print  If set to `true` enables pretty-printed output
                   in the ClojureScript files created by the Hoplon compiler."
  [c cache bool "In-memory caching of compiled output."
   pp pretty-print bool "Pretty-print CLJS files created by the Hoplon compiler."]
  (let [tmp (boot/temp-dir!)]
    (boot/with-pre-wrap fileset
      (println "Compiling Hoplon pages...")
      (boot/empty-dir! tmp)
      (let [in-files (boot/input-files fileset)
            hl-files (boot/by-ext [".hl"] in-files)]
        (doseq [hl hl-files]
          (let [f (boot/tmpfile hl)]
            (println "•" (.getPath f))
            (hl/compile-file f tmp :opts *opts*))))
      (-> fileset (boot/add-source tmp) boot/commit!))))