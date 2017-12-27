;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns hoplon.boot-hoplon.experimental
  {:boot/export-tasks true}
  (:require [boot.core        :as boot]
            [boot.pod         :as pod]
            [boot.util        :as util]
            [boot.file        :as file]
            [clojure.java.io  :as io]
            [clojure.string   :as string]
            [hoplon.boot-hoplon.refer :as refer]))

(def hoplon-pod
  (delay (pod/make-pod (->> (-> "hoplon/boot_hoplon/pod_deps.edn"
                                io/resource slurp read-string)
                            (update-in pod/env [:dependencies] into)))))

(defn bust-cache
  [path]
  (pod/with-eval-in @hoplon-pod
    (require 'hoplon.core)
    (hoplon.core/bust-cache ~path)))

(defn- by-path
  [paths tmpfiles]
  (boot/by-re (mapv #(re-pattern (str "^\\Q" % "\\E$")) paths) tmpfiles))

(boot/deftask bust-caches
  [p paths PATH #{str} "The set of paths to add cache-busting uuids to."]
  (let [tmp (boot/tmp-dir!)]
    (boot/with-pre-wrap fs
      (let [msg (delay (util/info "Busting cache...\n"))]
        (->> (boot/output-files fs)
             (by-path paths)
             (seq)
             (reduce (fn [fs {:keys [path]}]
                       @msg
                       (util/info "• %s\n" path)
                       (boot/mv fs path (bust-cache path)))
                     fs))))))

(def ^:private bogus-cljs-files #{"deps.cljs"})

(boot/deftask ^{:deprecated true} ns+
  "DEPRECATED. Extended ns declarations in CLJS."
  []
  (let [prev-fileset (atom nil)
        tmp-cljs+    (boot/tmp-dir!)]
    (boot/with-pre-wrap fileset
      (let [cljses    (->> (boot/fileset-diff @prev-fileset fileset)
                           (boot/input-files)
                           (boot/by-ext [".cljs"])
                           (remove (comp bogus-cljs-files boot/tmp-path))
                           (group-by boot/tmp-path))
            cljsdep   (->> cljses (keys) (refer/sort-dep-order))
            add-tmp!  (fn [fs]
                        (-> fs (boot/add-resource tmp-cljs+) boot/commit!))
            desc      (delay (util/info "Rewriting ns+ declarations...\n"))
            say-it    (fn [path] @desc (util/info "• %s\n" path))]
        (reset! prev-fileset fileset)
        (doseq [path cljsdep]
          (let [f (io/file tmp-cljs+ path)]
            (when (.exists f) (io/delete-file f))))
        (loop [[path & paths] cljsdep fs (add-tmp! fileset)]
          (if-not path fs
            (let [modtime (.lastModified (boot/tmp-file (first (cljses path))))]
              (refer/rewrite-ns-path say-it modtime tmp-cljs+ path)
              (recur paths (add-tmp! fs)))))))))
