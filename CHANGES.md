## 2.0.1 -- 2.0.2

* Use relative URIs to load JavaScript; no need for `:base-dir` option.
* Fix bug with `:outdir-out` handling that broke cljs when no `:optimizations`
  present.
* Change default for `:outdir-out` to `"out"` from `nil`.

## 2.0.0 -- 2.0.1

* Change default for `:work-dir` to `".hlisp-work-dir"` from `"hlwork"`.
* Rewrite file watching code; way faster now.
* Replace clj `pprint` with just `pr-str`; Compiling is way faster now.
* Improve robustness of HTML pretty printer by preprocessing more.
* Simplify options map (`:hlisp` key in user _project.clj_).
* Clean up plugin startup code.
* Update to newer clojurescript.
