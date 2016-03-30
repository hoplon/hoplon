(ns hoplon.index-test
  (:use
    [clojure.test]
    [clj-webdriver.taxi]
    [test-util.fixtures :as fixtures]))

(use-fixtures :each fixtures/selenium-driver!)

(deftest first-test
  (is (= "hello world" (text (element "h1")))))

(deftest all-html
  "All HTML tags can be output by Hoplon"
  ; https://developer.mozilla.org/en-US/docs/Web/HTML/Element
  (doseq [tag [ ; http://www.html-5-tutorial.com/all-html-tags.htm
                ; "DOCTYPE!"
                "a"
                "a[href=\"http://hoplon.io/\"]"

                "abbr"
                "abbr[title=\"Cascading Style Sheet\"]"

                "address"

                "area"
                "area[shape=\"rect\"][coords=\"0,0,100,100\"][href=\"http://hoplon.io\"][alt=\"click me\"]"

                "article"

                "aside"

                "audio"
                "audio[controls]"

                "b"

                "base"
                "base[href=\"http://hoplon.io/images/\"]"

                "bdi"

                "bdo"
                "bdo[dir=\"rtl\"]"

                "html"
                ; metadata
                "head"
                "link"
                "meta"
                "style"
                "title"
                ; content sectioning
                "footer"
                "header"
                "h1"
                "h2"
                "h3"
                "h4"
                "h5"
                "h6"
                "hgroup"
                "nav"
                "section"
                ; text content
                "dd"
                "div"
                "dl"
                "dt"
                "figcaption"
                "figure"
                "hr"
                "li"
                "main"
                "ol"
                "p"
                "pre"
                "ul"
                ; inline text semantics
                "br"
                "cite"
                "code"
                "data"
                "dfn"
                "em"
                "i"
                "kbd"
                "mark"
                "q"
                "rp"
                "rt"
                "rtc"
                "ruby"
                "s"
                "samp"
                "small"
                "span"
                "strong"
                "sub"
                "sup"
                "time"
                "u"
                "var"
                "wbr"
                ; image and multimedia
                "map"
                "track"
                "video"
                ; embedded content
                "embed"
                "object"
                "param"
                "source"
                ; scripting
                "canvas"
                "noscript"
                "script"
                ; demarcating edits
                "del"
                "ins"
                ; table content
                "caption"
                "col"
                "colgroup"
                "table"
                "tbody"
                "td"
                "tfoot"
                "th"
                "thead"
                "tr"
                ; forms
                "button"
                "datalist"
                "fieldset"
                "form"
                "input"
                "keygen"
                "label"
                "legend"
                "meter"
                "optgroup"
                "option"
                "output"
                "progress"
                "select"
                "textarea"
                ; interactive components
                "details"
                "dialog"
                "menu"
                "menuitem"
                "summary"]]
    (is (exists? tag))))
    ; (Thread/sleep 5000)))
