(ns hoplon.protocol)

;; Hoplon Constructor ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IHoplonConstructor
  (-ctor! [this] [this key]))

;; Hoplon Element ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IHoplonElement
  (-set-attributes! [this kvs])
  (-set-styles!     [this kvs])
  (-append-child!   [this child])
  (-remove-child!   [this child])
  (-replace-child!  [this new existing])
  (-insert-before!  [this new existing]))

;; Hoplon Attribute ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IHoplonAttribute
  (-attr! [this elem value]))

;; Hoplon Node ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IHoplonNode
  (-node [this]))
