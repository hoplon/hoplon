(ns hoplon.spec
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::pairs
  #(-> % count even?))

(spec/def ::attr-kids
  (spec/cat
    :attr :clojure.core.specs.alpha/binding-form
    :kids (spec/? :clojure.core.specs.alpha/binding-form)))

(spec/def ::forms
  (spec/nilable any?))

(spec/def ::prepost
  (spec/and map? (spec/keys :opt-un [::pre ::post])))

(spec/def ::elem
  (spec/cat :bindings ::attr-kids :prepost (spec/? ::prepost) :forms (spec/* ::forms)))

(spec/def ::defelem
  (spec/cat :name simple-symbol? :docstring (spec/? string?) :forms (spec/* ::forms)))

(spec/def ::bindings-kw
  (spec/and keyword? (partial = :bindings)))

(spec/def ::binding-cell
  (spec/cat :binding :clojure.core.specs.alpha/binding-form :cell ::forms))

(spec/def ::loop-tpl
  (spec/cat :bindings-kw ::bindings-kw :bindings ::binding-cell :forms ::forms))

(spec/def ::for-tpl
  (spec/cat :bindings ::binding-cell :forms (spec/* ::forms)))

(spec/def ::if-tpl
  (spec/cat :predicate any? :consequent ::forms :alternative (spec/? ::forms)))

(spec/def ::when-tpl
  (spec/cat :predicate any? :consequent ::forms))

(spec/def ::cond-tpl
  (spec/and ::pairs (spec/coll-of ::forms)))

(spec/def ::case-tpl
  (spec/cat :expr ::forms :cases (spec/* ::forms) :default ::forms))
