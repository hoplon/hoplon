## 2.0.0 -- 2.0.1

* Move work dir from _hlwork_ to _.hlisp-work-dir_.
* Rewrite file watching code.
* Replace clj pprint with just pr-str; pprint was too slow.
* Improve robustness of HTML pretty printer by preprocessing more.
* Simplify options map (:hlisp key in user project.clj).
* Clean up plugin startup code.
* Update to newer clojurescript.
