# Getting Started

Hoplon applications are be built using the 
[boot](http://github.com/tailrecursion/boot)
build tool. The following `boot.clj` file is a good starting point:

```clojure
{:project       my-hoplon-project
 :version       "0.1.0-SNAPSHOT"
 :dependencies  [[tailrecursion/boot.task "0.1.0-SNAPSHOT"]
                 [tailrecursion/hoplon "1.1.0-SNAPSHOT"]]
 :require-tasks #{[tailrecursion.boot.task :refer :all]}
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

For the purposes of this document (as specified in the `boot.clj` file above)
the source paths are organized as follows:

| Directory    | Contents                                          |
|--------------|---------------------------------------------------|
| _src/html_   | Hoplon source files, organized in directories reflecting the application's HTML page structure. |
| _src/static_ | Static content (CSS files, images, etc.), organized in directories reflecting the application's HTML page structure. |
| _src/cljs_   | ClojureScript library source files.               |
| _src/clj_    | Clojure source files.                             |

### Library And Package Management

Hoplon projects can depend on maven artifacts, specified in the `:dependencies`
key of the project _boot.clj_ file. These jar files may contain any of the
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

_src/html/index.html.hl_

```html
<script type="text/hoplon">
  (ns hello.index)
</script>   

<html>
  <head></head>
  <body>
    <h1 id="main" style="color:red">Hello world</h1>
  </body>
</html>
```

Note the `.html.hl` extension: all files ending in `.hl` will be compiled by
the Hoplon compiler, and the `.html.hl` ending tells Hoplon that the source
file format is HTML markup. Hoplon can also compile source files with the
`.cljs.hl` extension, which indicates that the source file format is
ClojureScript forms (s-expressions) instead of HTML markup. This is covered in
detail below.

## S-Expression Syntax

Since HTML markup is a tree structure it can be expressed as [s-expressions]
(http://en.wikipedia.org/wiki/S-expression). For example, this HTML markup

```html
<form>
  <input>
  <input>
</form>
```

is syntactically equivalent to this s-expression

```clojure
(form (input) (input))
```

Conversely, the s-expression

```clojure
(map identity coll)
```

could be represented equivalently in HTML markup

```html
<map><identity/><coll/></map>
```

With that in mind, the Hello World example can be translated into s-expression
syntax. The Hoplon compiler can compile HTML source in this format, as well.

_src/html/index.cljs.hl_

```clojure
(ns hello.index)

(html
  head
  (body
    (h1 {:id "main" :style "color:red"} "Hello world")))
```

When the application is compiled the output files _resources/public/index.html_
and _resources/public/main.js_ are produced.

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

### HTML As S-Expression Syntax Rules

* **Elements:** An element is represented as a list enclosed in parentheses.
* The first item in the list must be the element's tag name.
* The second item may be an attribute map with keyword keys if the element
  has attribute nodes.
* The rest of the items are the element's children and may be text or element
  nodes.
* Text nodes are represented as strings or `($text "Value")`.
* Parentheses may be omitted around elements which have no children or
  attributes.
* Comment nodes are represented as `($comment "the comment")`.
* The special form `spliced` acts like a combination of Clojure's
  `unquote-splicing` and `apply` forms&mdash;children of the `spliced` form
  are appended to its parent. For example, `(div (spliced (p "1") (p "2")))`
  is equivalent to `(div (p "1") (p "2"))`.

### ClojureScript CSS Literal Syntax

When editing HTML as s-expressions the compiler will also parse `<style>`
elements containing a simple ClojureScript CSS literal syntax:

```clojure
(ns hello.index)

(html
  (head
    (style
      [body > h1], [div p span], [:#main]
      {:border "1px solid blue"}))
  (body
    (h1 {:id "main" :style "color:red"} "Hello world")))
```

The ClojureScript CSS syntax follows the following conventions:

* Selectors are vectors of symbols and/or keywords, the names of which will
  be used in the output.
* Declaration blocks are maps with keyword keys and string values.

### HTML-As-ClojureScript-As-HTML

The equivalence of HTML and s-expression syntax allows the representation
of HTML documents as s-expressions, and the representation of ClojureScript
s-expression source code as HTML documents, as shown above. The further
step of adding HTML primitives to the ClojureScript environment in which
the page is evaluated provides the semantics of HTML, as well.

HTML primitives are implemented as ClojureScript text and element node types.
Each of the [HTML5 elements](https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/HTML5/HTML5_element_list)
is defined, i.e. `a`, `div`, `span`, `p`, etc. The ClojureScript element node
type has the following properties:

* They are self-evaluating. There is no `render` function.
* They are immutable. Operations on a node return a new node and do not alter
  the original.
* They can be applied as functions. Applying a HTML node to arguments appends
  those arguments to the node as children, returning a new immutable node.
* Attributes can be accessed via map functions. Use `get`, `assoc`, etc. to
  access element attributes.
* Children can be accessed via vector functions. Use `nth`, `peek`, `into`, etc.
  to access child nodes.

This implementation provides a literal representation of HTML as code, and of
code as HTML. This allows the use of macros in HTML documents, and seamless
templating as templates in this environment are simply functions that return
nodes.

_src/html/func.cljs.hl_

```clojure
(ns hello.func)

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

When _resources/public/func.html_ is viewed in the browser the list items,
headings, and paragraphs will be seen in the resulting HTML. As always, the
same page can be represented as HTML markup:

_src/html/func2.html_

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
ships with Hoplon. The `tailrecursion.hoplon.reactive` library ties FRP data
structures from [Javelin](http://github.com/tailrecursion/javelin) to the DOM.
Consider the following program:

_src/html/react1.cljs.hl_

```clojure
(ns hello.react1
  (:require-macros
    [tailrecursion.javelin.macros   :refer [cell]]
    [tailrecursion.hoplon.macros    :refer [with-frp]])
  (:require
    [tailrecursion.javelin          :as j]
    [tailrecursion.hoplon.reactive  :as r]))

(def clicks (cell 0))

(html
  (head
    (title "Reactive Attributes: Example 1"))
  (body
    (with-frp
      (h1 {:on-click [#(swap! clicks inc)]} "click me")
      (p {:do-text [(format "You've clicked %s times, so far." clicks)]}))))
```

Clicking on the "click me" element causes the p element to update, its text
reflecting the number of times the user has clicked so far. Note that the
p element's text updates _reactively_, responding automatically to the updated
value of the `clicks` cell.

### Reactive Attributes

In the example above the DOM was wired up to the underlying Javelin cells
via the `:on-click` and `:do-text` attributes on DOM elements. In general,
reactive attributes are divided into two categories: input and output.
Input attributes connect user input events (click, keypress, mouseover, etc.)
to cell values via a callback function. These attributes all start with the
prefix `on-`. Output attributes link the state of DOM elements to the state
of the underlying Javelin cells via ClojureScript expressions. These attributes
all start with the prefix `do-`.

| Attribute                 | Description |
|---------------------------|-------------|
| `:loop [looper i & args]` | See the [thing-looper](#) section below. |
| `:on-<event> [callback]`  | Adds handler `callback` to be called when _event_ is triggered on the element. Supported events are: _change_, _click_, _dblclick_, _error_, _focus_, _focusin_, _focusout_, _hover_, _keydown_, _keypress_, _keyup_, _load_, _mousedown_, _mouseenter_, _mouseleave_, _mousemove_, _mouseout_, _mouseover_, _mouseup_, _ready_, _scroll_, _select_, _submit_, and _unload_. The callback must be a function of one argument: the browser event object. |
| `:do-value [expr]`        | Sets the `value` of the element to the value of `expr`. The special values `true` and `false` will check or uncheck checkboxes. |
| `:do-attr [attr expr]`    | Sets the attribute `attr` to the value of `expr`. The special values `true` and `false` add or remove the attribute. |
| `:do-class [class expr]`  | Adds or removes the CSS class `class` depending on whether `expr` is truthy or falsy. |
| `:do-css [prop expr]`     | Sets the css property `prop` to the value of `expr`. |
| `:do-toggle [expr]`       | Toggles visibility of the element according to the truthiness of `expr`. |
| `:do-slide-toggle [expr]` | Toggles visibility of the element according to the truthiness of `expr` using a sliding animation. |
| `:do-fade-toggle [expr]`  | Toggles visibility of the element according to the truthiness of `expr` using a fading animation. |
| `:do-focus [expr]`        | Triggers the `focus` event on the element when `expr` changes to a truthy value. |
| `:do-select [expr]`       | Triggers the `select` event on the element when `expr` changes to a truthy value. |
| `:do-focus-select [expr]` | Triggers the `focus` and `select` events on the element when `expr` changes to a truthy value. |
| `:do-text [expr]`         | Sets the element's text to the value of `expr`. |

#### Custom Reactive Attributes

The output attributes can be extended by adding to the
`tailrecursion.hoplon.reactive/do!` multimethod. For example, adding a `:foo`
dispatch method will enable the use of the `:do-foo` attribute. Look at the
implementations of the above attributes in the
[source file](https://github.com/tailrecursion/hoplon/blob/master/src/tailrecursion/hoplon/reactive.cljs)
for examples and ideas.

