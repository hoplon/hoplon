# Getting Started

Hoplon web application frontends are built from source files by the Hoplon
compiler. The Hoplon compiler compiles source files with the `.html.hl` and
`.cljs.hl` extensions, emitting static HTML pages and ClojureScript source
files. The ClojureScript sources are then compiled into a `main.js` file that
is loaded from the HTML pages. The resulting output files can then be served
from a web server's document root.

#### Hoplon Source File Extensions

Hoplon pages can be written using either the familiar HTML markup or the
Clojure [s-expression][2] syntax. Use the file extension to indicate which
syntax the source file contains:

* `.html.hl`: HTML markup syntax
* `.cljs.hl`: [s-expression][2] syntax

## Building a Hoplon Application

Hoplon applications are built using the [boot][1] build tool. The following
`boot.edn` file is a good starting point:

```clojure
{:project       my-hoplon-project
 :version       "0.1.0-SNAPSHOT"
 :dependencies  [[org.clojure/clojurescript "0.0-1859"]
                 [tailrecursion/boot.task "0.1.1"]
                 [tailrecursion/hoplon "1.1.4"]]
 :require-tasks #{[tailrecursion.boot.task :refer :all]
                  [tailrecursion.hoplon.boot :refer :all]}
 :src-paths     #{"src/html" "src/clj" "src/cljs"}
 :src-static    #{"src/static"}
 :public        "resources/public"}
```

When the project is built, HTML and JavaScript files will be created and put
in the `resources/public` directory.

```bash
# build once and exit
$ boot hoplon

# watch source paths for changes and rebuild as necessary
$ boot watch hoplon
```

### Compiler Source Directories

For the purposes of this document (as specified in the `boot.edn` file above)
the source paths are organized as follows:

| Directory    | Contents                                          |
|--------------|---------------------------------------------------|
| _src/html_   | Hoplon source files, organized in directories reflecting the application's HTML page structure. |
| _src/static_ | Static content (CSS files, images, etc.), organized in directories reflecting the application's HTML page structure. |
| _src/cljs_   | ClojureScript library source files.               |
| _src/clj_    | Clojure source files.                             |

### Library And Package Management

Hoplon projects can depend on maven artifacts, specified in the `:dependencies`
key of the project _boot.edn_ file. These jar files may contain any of the
following:

* Clojure namespaces (ClojureScript macros are written in Clojure).
* ClojureScript namespaces to be used in the project.
* Raw JavaScript source files to be prepended (in dependency order) to the
  _main.js_ output file.
* Google Closure Compiler ready JavaScript source and extern files.

Note that JavaScript dependency jar files must be prepared a certain way,
described [here](#).

## Hello World

The simplest example application looks almost exactly like a standard HTML web
page, with the exception of an unfamiliar script tag containing a namespace
declaration. All HTML source files in a Hoplon application must declare a
namespace. This is because the HTML contained in the document body is going to
be _evaluated_ as ClojureScript in the browser.

```html
<script type="text/hoplon">
  (ns hello.index
    (:require tailrecursion.hoplon tailrecursion.javelin)
    (:require-macros
      [tailrecursion.javelin :refer [refer-all]]))

  (refer-all tailrecursion.hoplon)
  (refer-all tailrecursion.javelin)
</script>   

<html>
  <head></head>
  <body>
    <h1 id="main" style="color:red">Hello world</h1>
  </body>
</html>
```

## S-Expression Syntax

Since HTML markup is a tree structure it can be expressed as [s-expressions][2].
For example, this HTML markup

```html
<form><input><input></form>
```

is syntactically equivalent to this s-expression

```clojure
(form input input)
```

With that in mind, the Hello World example can be translated into s-expression
syntax. (This is, in fact, the first pass when the file is compiled.)

```clojure
(ns hello.index
  (:require tailrecursion.hoplon tailrecursion.Javelin)
  (:require-macros
    [tailrecursion.javelin :refer [refer-all]]))

(refer-all tailrecursion.hoplon)
(refer-all tailrecursion.javelin)

(html
  head
  (body
    (h1 {:id "main" :style "color:red"} "Hello world")))
```

Note that the script element has been removed in the sexp version. The script
element in the HTML version serves simply to splice the lisp expressions it
contains into the surrounding HTML markup. This is necessary when working in
HTML markup because ClojureScript source code is not strictly s-expressions; it
is made up of lists, maps, vectors, reader macros, etc., and names which are
valid in ClojureScript may contain characters which would crash a sane HTML
parser (the function `clj->js`, for example, which cannot be represented in
HTML markup as `<clj->js/>`).

In general, Hoplon programs can be represented equivalently in either
HTML or ClojureScript syntax. This is an important point for designers
and developers who rely on development tools to get the maximum level
of productivity.

### ClojureScript HTML Syntax Rules

* **Elements:** An element is represented as a list enclosed in parentheses.
  * The first item in the list must be the element's **tag name**.
  * The second item may be an **attribute map** with keyword keys if the
    element has attribute nodes.
  * The rest of the items are the element's **children** and may be text or
    element nodes.
  * Parentheses **may be omitted** around elements which have no children or
    attributes.
* **Text Nodes:** Text nodes are represented as strings or `($text "Value")`.
* **Comment Nodes:** Comment nodes are represented as `($comment "the comment")`.
* **Splicing:** The special form `spliced` acts like a combination of Clojure's
  `unquote-splicing` and `apply` forms&mdash;children of the `spliced` form
  are appended to its parent. For example, `(div (spliced (p "1") (p "2")))`
  is equivalent to `(div (p "1") (p "2"))`.

### HTML-As-ClojureScript-As-HTML

The equivalence of HTML and s-expression syntax allows the representation
of HTML documents as s-expressions, and the representation of ClojureScript
s-expression source code as HTML documents, as shown above. The further
step of adding HTML primitives to the ClojureScript environment in which
the page is evaluated provides the semantics of HTML, as well.

HTML primitives are implemented as ClojureScript text and element node types.
Each of the [HTML5 elements][3] is defined, i.e. `a`, `div`, `span`, `p`, etc.
The ClojureScript element node type has the following properties:

* **They are self-evaluating.** There is no `render` function.
* **They are immutable.** Operations on a node return a new node and do not
  alter the original.
* **They can be applied as functions.** Applying a HTML node to arguments
  appends those arguments to the node as children, returning a new immutable
  node.
* **Attributes can be accessed via map functions.** Use `get`, `assoc`, etc. to
  access element attributes.
* **Children can be accessed via vector functions.** Use `nth`, `peek`, `into`,
  etc. to access child nodes.

This implementation provides a literal representation of HTML as code, and of
code as HTML. This allows the use of macros in HTML documents, and seamless
templating as templates in this environment are simply functions that return
nodes.

```clojure
(ns hello.func
  (:require tailrecursion.hoplon tailrecursion.javelin)
  (:require-macros
    [tailrecursion.javelin :refer [refer-all]]))

(refer-all tailrecursion.hoplon)
(refer-all tailrecursion.javelin)

(defn fancyitem [heading body]
  (li
    (h2 heading)
    (p body)))
    
(html
  (head
    (title "Hello Functions"))
  (body
    (h1 "Hello Functions")
    (ul
      (fancyitem (span "Item 1") (span "This is the first item."))
      (fancyitem (span "Item 2") (span "This is the second item.")))))
```

As always, the same page can be represented as HTML markup:

```html
<script type="text/hoplon">
  (ns hello.func2)
      
  (defn fancyitem [heading body]
    (li
      (h2 heading)
      (p body)))
</script>

<html>
  <head>
    <title>Hello Functions</title>
  </head>
  <body>
    <h1>Hello Functions</h1>
    <ul>
      <fancyitem>
        <span>Item 1</span>
        <span>This is the first item.</span>  
      </fancyitem>
      <fancyitem>
        <span>Item 2</span>
        <span>This is the second item.</span>  
      </fancyitem>
    </ul>
  </body>
</html>
```

For brevity's sake the rest of the documentation will present examples as
either HTML or s-expressions, with the implication that either can be
easily represented in the other syntax if desired. Of course some care
must be taken when using HTML syntax that tag names do not contain invalid
characters, etc.

## Functional Reactive Programming

An example of how macros can be used to advantage is the `with-frp` macro that
ships with Hoplon. It ties the FRP reference type from [Javelin][4] to the DOM
such that DOM elements update reactively when the underlying [Javelin][4] cells
change and [Javelin][4] cells are updated in response to user input (events).
Consider the following program:

```clojure
(ns hello.react1
  (:require tailrecursion.javelin tailrecursion.hoplon)
  (:require-macros
    [tailrecursion.javelin  :refer [refer-all defc]]
    [tailrecursion.hoplon   :refer [with-frp]]))

(refer-all tailrecursion.hoplon)
(refer-all tailrecursion.javelin)

(defc clicks 0)

(html
  (head
    (title "Reactive Attributes: Example 1"))
  (body
    (with-frp
      ;; underlying cells wired to DOM using the :do-text attribute
      (p {:do-text [(format "You've clicked %s times, so far." clicks)]})

      ;; underlying cells wired to DOM using interpolated text node
      (p "If you click again you'll have clicked ~(inc clicks) times.")

      ;; user input (click event) wired to change underlying cells
      (button {:on-click [#(swap! clicks inc)]} "click me"))))
```

Clicking on the "click me" button causes the paragraph element to update, its
text reflecting the number of times the user has clicked so far. Note that the
paragraph's text updates _reactively_ according to a _formula_&mdash;responding
automatically to the updated value of the `clicks` cell.

### Reactive Attributes

In the example above the DOM was wired up to the underlying [Javelin][4] cells
via the `:on-click` and `:do-text` attributes on DOM elements. In general,
reactive attributes are divided into two categories: **input** and **output**.

##### Input Attributes
  * start with the prefix `on-`.
  * connect DOM events (click, keypress, mouseover, etc.) to cell values via a
    callback function.
  * are extended by calling the `#'tailrecursion.hoplon/add-event!` function.
    Examples [here][5].
  
##### Output Attributes
  * start with the prefix `do-`.
  * link the state of DOM elements to the state of the underlying [Javelin][4]
    cells via formulas.
  * are extended by providing a method for the `#'tailrecursion.hoplon/do!`
    multimethod. Examples [here][5].

| Attribute                 | Description |
|---------------------------|-------------|
| `:loop [looper i & args]` | See the [thing-looper][6] section below. |
| `:on-<event> [callback]`  | Adds handler `callback` to be called when _event_ is triggered on the element. Supported events are: _change_, _click_, _dblclick_, _error_, _focus_, _focusin_, _focusout_, _hover_, _keydown_, _keypress_, _keyup_, _load_, _mousedown_, _mouseenter_, _mouseleave_, _mousemove_, _mouseout_, _mouseover_, _mouseup_, _ready_, _scroll_, _select_, _submit_, and _unload_. The callback must be a function of one argument: the browser event object. |
| `:do-value [expr]`        | Sets the `value` of the element to the value of the formula `expr`. The special values `true` and `false` will check or uncheck checkboxes. |
| `:do-attr [attr expr]`    | Sets the attribute `attr` to the value of the formula `expr`. The special values `true` and `false` add or remove the attribute. |
| `:do-class [class expr]`  | Adds or removes the CSS class `class` depending on whether the value of the formula `expr` is truthy or falsy. |
| `:do-css [prop expr]`     | Sets the css property `prop` to the value of the formula `expr`. |
| `:do-toggle [expr]`       | Toggles visibility of the element according to the truthiness of the value of the formula `expr`. |
| `:do-slide-toggle [expr]` | Toggles visibility of the element according to the truthiness of the value of the formula `expr` using a sliding animation. |
| `:do-fade-toggle [expr]`  | Toggles visibility of the element according to the truthiness of the value of the formula `expr` using a fading animation. |
| `:do-focus [expr]`        | Triggers the `focus` event on the element when the value of the formula `expr` changes to a truthy value. |
| `:do-select [expr]`       | Triggers the `select` event on the element when the value of the formula `expr` changes to a truthy value. |
| `:do-focus-select [expr]` | Triggers the `focus` and `select` events on the element when the value of the formula `expr` changes to a truthy value. |
| `:do-text [expr]`         | Sets the element's text to the value of `expr`. |

### Thing Looper

Most applications have some DOM structure that is repeated a number of times,
once for each item in an array. This could be a list of to-do items which would
be inserted into an ordered list, for example. The thing-looper mechanism is
provided to accomplish this in a way that avoids coupling between data and DOM.

For example:

```clojure
(ns html.index
  (:require tailrecursion.javelin tailrecursion.hoplon)
  (:require-macros
    [tailrecursion.javelin :refer [cell= defc defc=]]
    [tailrecursion.hoplon :refer [with-frp]]))

(refer-all tailrecursion.hoplon)
(refer-all tailrecursion.javelin)

(defc people
  [{:first "bob" :last "smith"}
   {:first "joe" :last "blow"}])

(def loop-people
  (thing-looper
    people
    (fn [people i person]
      [(cell= (:first person))
       (cell= (:last person))])))

(html
  (head
    (title "looping"))
  (body
    (h1 "people")
    (ul {:loop [loop-people i first last]}
      (li "~{last}, ~{first}"))))
```

As can be seen in the example above, the thing looper mechanism consists of two
parts: the looper definition and the loop template, denoted by the `:loop`
attribute. The different parts can be in different files, even. For instance,
the looper definition can be in a library that is shared by multiple frontends,
each one requiring the library and having its own loop template.

##### Looper Definition
* fn takes three arguments:
  * the cell containing the items
  * the index of the current item
  * a cell containing the current item
* fn returns a vector of cells which will be passed as arguments to the `:loop`
  template.

##### Loop Template
* denoted by the `:loop` attribute, which takes the following arguments
  * the looper to loop over
  * a binding for the index of the current item (an int, not a cell)
  * bindings for the cells provided by the looper fn
* one child: a template inside of which the bindings in the `:loop` argument can
  be referenced.

Note that the templates are fully reactive: swapping the `people` cell will
cause the `li` elements in the list to update as necessary, automatically.

```clojure
(swap! people assoc-in [0 :last] "jones")
```

The list updates itself, with the first name in the list now reading
"jones, bob" instead of "smith, bob".

### The N-Things Problem

FIXME

[1]: https://github.com/tailrecursion/boot
[2]: http://en.wikipedia.org/wiki/S-expression
[3]: https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/HTML5/HTML5_element_list
[4]: https://github.com/tailrecursion/javelin
[5]: https://github.com/tailrecursion/hoplon/blob/master/src/tailrecursion/hoplon.cljs
[6]: #thing-looper
