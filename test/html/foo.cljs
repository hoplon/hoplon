(html

  (head
    (title "testing 123"))

  (body

    (ns holyshit2
      (:use
        [flapjax.core :only [sync-e]]
        [mytest.ui    :only [make-radio make-tabs]])
      (:use-macros
        [hlisp.macros :only [def-values deftpl tpl]]))

    (defn page-tpl1
      [heading message]
      (div
        (h1 heading)
        (p message)))

    (js/alert "hello foo")

    (page-tpl1
      "Hello, world."
      "This is a test.")))
