(ns clj-kondo.hoplon
  (:require [clj-kondo.hooks-api :as api]))

(defn hoplon-core-defelem
  [{:keys [node]}]
  (let [[_defelem name & forms]  (:children node)
        [docstr & [args & body]] (if (api/string-node? (first forms))
                                   forms
                                   (concat [""] forms))]
    {:node (api/list-node
            (list*
             (api/token-node 'defn)
             name
             docstr
             (api/vector-node
              [(api/token-node '&) args])
             body))}))

(defn hoplon-core-elem
  [{:keys [node]}]
  (let [[_elem & [args & body]] (:children node)]
    {:node (api/list-node
            (list*
             (api/token-node 'fn)
             (api/vector-node
              [(api/token-node '&) args])
             body))}))

(defn hoplon-core-loop-tpl
  [{:keys [node]}]
  (let [[_loop-tpl _bindings-kw bind & body] (:children node)]
    {:node (api/list-node
            (list*
             (api/token-node 'for)
             bind
             body))}))
