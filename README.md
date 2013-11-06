<img src="img/Hoplite.jpg">

# Hoplon

Hoplon is a set of tools and libraries for making web applications. Hoplon
provides a compiler for web application frontend development, and includes
the following libraries as dependencies to complete the stack:

* [Javelin][1]: a spreadsheet-like dataflow library for managing client
  state. Hoplon tightly integrates with Javelin to reactively bind DOM
  elements to the underlying Javelin cell graph.
* [Castra][2]: a full-featured RPC library for Clojure and
  ClojureScript, providing the serverside environment.
* [Cljson][3]: an efficient method for transferring Clojure/ClojureScript
  data between client and server. Castra uses cljson as the underlying
  transport protocol.

### Example

```xml
<script type="text/hoplon">
  ;; Page declaration specifies output file path.
  (page index.html)
  
  ;; definitions are optional
  (defn my-list [& items]
    (div
      :class "my-list"
      (apply ul (map #(li (div :class "my-list-item" %)) items))))

  (def clicks (cell 0))
</script>
    
<html>
  <head>
    <title>example page</title>
  </head>
  <body>
    <h1>Hello, Hoplon</h1>
    
    <!-- an HTML syntax call to the my-list function -->
    <my-list>
      <span>first thing</span>
      <span>second thing</span>
    </my-list>

    <!-- using FRP to link DOM and Javelin cells -->
    <p><text>You've clicked ~{clicks} times, so far.</text></p>
    <button on-click="{{ #(swap! clicks inc) }}">click me</button>
  </body>
</html>
```

Or, equivalently:

```clojure
(page index.html)

(defn my-list [& items]
  (div
    :class "my-list"
    (apply ul (map #(li (div :class "my-list-item" %)) items))))

(def clicks (cell 0))

(html
  (head
    (title "example page"))
  (body
    (h1 "Hello, Hoplon")

    (my-list
      (span "first thing")
      (span "second thing"))

    (p (text "You've clicked ~{clicks} times, so far."))
    (button :on-click #(swap! clicks inc) "click me")))
```

### Demos

* [Hoplon demo applications repository][5]

### Dependency

Artifacts are published on [Clojars][4]. 

```clojure
[tailrecursion/hoplon "3.2.1"]
```

```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>hoplon</artifactId>
  <version>3.2.1</version>
</dependency>
```

### Documentation

* [Getting Started][9]
* ~~[Configuration][9]~~
* ~~[API Documentation][9]~~
* [Design Document][6]

## Getting Started

Hoplon web application frontends are built from source files by the Hoplon
compiler. The Hoplon compiler compiles source files with the `.html.hl` and
`.cljs.hl` extensions, emitting static HTML pages and ClojureScript source
files. The ClojureScript sources are then compiled into a `main.js` file that
is loaded from the HTML pages. The resulting output files can then be served
from a web server's document root.

#### Hoplon Source File Extensions

Hoplon pages can be written using either the familiar HTML markup or the
Clojure [s-expression][12] syntax. Use the file extension to indicate which
syntax the source file contains:

* `.html.hl`: HTML markup syntax
* `.cljs.hl`: [s-expression][12] syntax

### Building a Hoplon Application

Hoplon applications are built using the [boot][11] build tool. The following
`boot.edn` file is a good starting point:

```clojure
{:project       my-hoplon-project
 :version       "0.1.0-SNAPSHOT"
 :dependencies  [[org.clojure/clojurescript "0.0-1859"]
                 [tailrecursion/boot.task "0.1.3"]
                 [tailrecursion/hoplon "3.2.1"]]
 :require-tasks #{[tailrecursion.boot.task :refer :all]
                  [tailrecursion.hoplon.boot :refer :all]}
 :src-paths     #{"src/html" "src/clj" "src/cljs"}
 :src-static    #{"resources/static"}
 :public        "resources/public"}
```

The project is built by doing the following in a terminal in the project dir:

```bash
# build once and exit
$ boot hoplon

# watch source paths for changes and rebuild as necessary
$ boot watch hoplon
```

#### Compiler Source Directories

For the purposes of this document (as specified in the `boot.edn` file above)
the project paths are organized as follows:

| Directory           | Contents                                              |
|---------------------|-------------------------------------------------------|
| _src/html_          | Hoplon source files.                                  |
| _src/cljs_          | ClojureScript library source files.                   |
| _src/clj_           | Clojure source files.                                 |
| _resources/static_  | Static content (CSS files, images, etc.), organized in directories reflecting the application's HTML page structure. |
| _resources/public_  | Compiled HTML and JavaScript files, with static content overlayed. |

This particular directory structure was chosen simply as an example. In practice
the source paths directory structure is completely arbitrary. Source files can
be organized in any way that makes sense for the project. Static files, of
course, need to have a directory structure that mirrors the desired output
directory structure.

#### Library And Package Management

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

### Hello World

The simplest example application looks almost exactly like a standard HTML web
page, with the exception of an unfamiliar script tag containing a [page
declaration][7]. This bit of code tells the Hoplon compiler where to put the
output file it will produce. The rest of the file is a normal web page, written
in the normal HTML syntax.

```html
<script type="text/hoplon">
  (page index.html)
</script>   

<html>
  <head></head>
  <body>
    <h1 id="main" style="color:red">Hello world</h1>
  </body>
</html>
```

The compiled page, _resources/public/index.html_, when viewed in the browser,
is a page with a red heading of "Hello world", as one would expect.

### S-Expression Syntax

Since HTML markup is a tree structure it can be expressed as lists of lists,
also known as [s-expressions][12].  For example, the HTML markup
`<form><input><input></form>` is syntactically equivalent to the s-expression
`(form (input) (input))`. With that in mind, the Hello World example can be
translated into s-expression syntax. The formal rules of s-expr as HTML syntax
are presented in the next section, but to see what it looks like:

```clojure
(page index.html)

(html
  (head)
  (body
    (h1 :id "main" :style "color:red" "Hello world")))
```

In general, Hoplon programs can be represented equivalently in either HTML or
ClojureScript syntax. The Hoplon compiler actually performs the conversion from
HTML syntax to ClojureScript forms as the first pass when the source file is in
HTML markup. This ability to consume either format is a nice affordance for
designers and developers who rely on development tools to maximize productivity.

#### ClojureScript As HTML

The equivalence of HTML and ClojureScript syntax is interesting and can be used
to generate HTML, like [Hiccup][18] does. What Hoplon provides goes farther to
include semantic equivalence, as well. This is accomplished by the Hoplon
ClojureScript runtime environment and its extensions to the JavaScript DOM
objects.

##### HTML

In HTML there are three types of primitives:

* element nodes
* text nodes
* attribute nodes

And there is one semantic form&mdash;append:

* element, text, and attribute nodes may be appended to element nodes
* denoted by nesting
  * attribute nodes nested in the opening tag
  * element and text nodes nested between the opening and closing tags

##### ClojureScript

This suggests the following s-exp representation for the HTML primitives:

* element nodes are lists enclosed in parentheses
  * tag name in function position (e.g. `html`, `div`, `span`, etc.)
  * attribute nodes as (optional) leading arguments
  * child element and/or text nodes as (optional) remaining arguments
* attribute nodes are either a map of keyword/value pairs or the alternating
  keyword/values themselves
* text nodes are bare strings or a list with the special `text` or `$text` tags.

Semantically, nesting in HTML directly maps to function application in
ClojureScript. Hoplon provides a ClojureScript environment where:

* the browser `Element` type is extended to implement `IFn` such that the HTML
  append semantic is preserved
* functions `html`, `div`, `span`, etc. are defined; each creates a DOM element
  with the associated tag and applies it to any arguments.

```clojure
;; Element with no attributes or children.
(div)

;; Elements with no attributes. Attribute maps may be omitted.
(div (hl))

;; Bare strings outside of attribute key/value pairs are text nodes.
(div
  (p "Paragraph 1")
  (p "Paragraph 2"))
;;    ^-- text node

;; Or text nodes may be explicitly created using the $text form.
(div
  (p ($text "Paragraph 1"))
  (p ($text "Paragraph 2")))
;;    ^-- explicitly create text node

;; An element with attributes and a child. Key/value pairs provided inline.
(div :foo "bar" :baz "baf" "hello")
;;   ^-----^----^-----^-.   ^-- text node
;;                      `-- key/value pairs

;; Attributes can also be a single map of key/values.
(div {:foo "bar" :baz "baf"} "hello")
;;   ^-- attribute map        ^-- text node

;; Inline attributes with Scheme-like indenting
(div
  :id "main"
  :class "component-wrapper"
  (form
    :action "foo.php"
    (label
      :for "first-name"
      "First Name")
    (input
      :type "text"
      :id "first-name"
      :name "first-name")
    (br)
    (label
      :for "last-name"
      "Last Name")
    (input
      :type "text"
      :id "last-name"
      :name "last-name")))

;; Other more interesting forms are possible. This example is equivalent to the
;; previous example when evaluated as ClojureScript expressions.
((div :id "main" :class "component-wrapper")
   ((form :action "foo.php")
      (label :for "first-name" "First Name")
      (input :type "text" :id "first-name" :name "first-name")
      (br)
      (label :for "last-name" "Last Name")
      (input :type "text" :id "last-name" :name "last-name")))
```

This last example is more than mere syntactic sugar. Here is the full power of
Lisp&mdash;a program that evaluates to an HTML document. This suggests the
possibility of producing HTML documents by evaluating programs written in HTML
markup (or equivalent s-expressions) _in the client_, which is exactly what
happens when a page in a Hoplon application is loaded.

#### Hoplon Pages

Web applications are necessarily organized into "pages". Each page is a single
HTML file that resides on a server or, in the bad old days, was generated
dynamically for each request. Hoplon applications are also organized this way.

A typical page might look like this:

```clojure
(page examples/sexp.html)

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

The page can be divided into three main parts, from top to bottom:

* the page declaration
* definitions and initialization code
* the page markup

The first form in the file must always be the page declaration, and the last
form must always be the page markup. In between may be a number of ClojureScript
expressions that might define vars needed elsewhere in the page, run setup or
initialization code, talk to the server, or anything else required to make the
page go.

#### Page Declaration

Each Hoplon source file must have a page declaration as its first form. The
page declaration

* determines the path of the output file relative to the webserver document
  root.
* declares a ClojureScript namespace for the page (the namespace name is
  obtained by [munging][17] the output file path).
* automatically adds `:require` and `:require-macros` clauses to refer all
  names and macros from the `tailrecursion.hoplon` and `tailrecursion.javelin`
  namespaces.
* may contain `:refer-clojure`, `:require` and/or `:require-macros` clauses.

For example:

```clojure
(page examples/lesson1/fractions.html
  ;;  ^-- REQUIRED output file path

  (:refer-clojure :exclude [nth name])
  ;;  ^-- OPTIONAL :refer-clojure clause

  (:require [needful.core :as needful :refer [nth name]])
  ;;  ^-- OPTIONAL :require clause

  (:require-macros [acme.super-transform :refer [uberdef]])
  ;;  ^-- OPTIONAL :require-macros clause
  )
```

#### Definitions And Initialization

Any ClojureScript expressions in the file between the page declaration and the
page markup are evaluated when the page has loaded and the DOM is ready, but
before the page markup is evaluated.

#### Page Markup

The last form in a Hoplon source file must always be the page markup. This is
the ClojureScript expression that will be evaluated when the page is loaded in
the browser; the result is immediately swapped into the page's DOM.

### Dataflow / Reactive Programming

Hoplon provides bindings for dataflow/reactive programming. The special
reactive attributes tie together [Javelin][14] cells and DOM elements such that
the state of DOM elements can be linked to the state of [Javelin][14] cells and
the values in [Javelin][14] input cells can be updated in response to user
interaction (events). Consider the following program:

```clojure
(page examples/frp.html)

(defc clicks 0)

(html
  (head
    (title "Reactive Attributes: Example 1"))
  (body
    ;; underlying cells wired to DOM using the :do-text attribute
    (p :do-text (cell= (format "You've clicked %s times, so far." clicks)))

    ;; underlying cells wired to DOM using interpolated text node via the `text` macro
    (p (text "If you click again you'll have clicked ~(inc clicks) times."))

    ;; user input (click event) wired to change underlying cells
    (button :on-click #(swap! clicks inc) "click me")))
```

Clicking on the "click me" button causes the paragraph element to update, its
text reflecting the number of times the user has clicked so far. Note that the
paragraph's text updates _reactively_ according to a _formula_&mdash;its text
is updated automatically whenever the value in the `clicks` cell changes.

#### Reactive Attributes

In the example above the DOM was wired up to the underlying [Javelin][14] cells
via the `:on-click` and `:do-text` attributes on DOM elements. In general,
reactive attributes are divided into two categories: **input** and **output**.

##### Input Attributes
  * start with the prefix `on-`.
  * connect DOM events to cell values via a callback function.
  * all events supported by jQuery are supported (custom events, too)
  * attribute value is the callback function `(fn [event-obj] ...)`
  
##### Output Attributes
  * start with the prefix `do-`.
  * link the state of DOM elements to the state of the underlying [Javelin][14]
    cells via a formula cell.
  * are extended by providing a method for the `#'tailrecursion.hoplon/do!`
    multimethod. Examples [here][15].
  * attribute value is the formula cell `(cell= ...)`

#### Template Looping

Most applications have some DOM structure that is repeated a number of times,
once for each item in an array. This could be a list of to-do items which would
be inserted into an ordered list, for example. The `loop-tpl` macro is provided
to accomplish this in a way that avoids coupling between the application state
and the DOM.

For example:

```clojure
(page examples/looped-template.html)

(defc todos
  [{:done false :todo-text "take out the garbage"}
   {:done false :todo-text "walk the dog"}
   {:done true  :todo-text "fix the car"}
   {:done true  :todo-text "pay the bills"}])

(html
  (head
    (title "Todo List"))
  (body
    (h1 "Things To Do Today")
    (ul
      (loop-tpl
        :size 4
        :binding [{:keys [done todo-text]} todos]
        (li
          :do-class (cell= {:done done})
          (text "~{todo-text}"))))))
```

#### The N-Things Problem

FIXME

## License

```
Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```

[1]: https://github.com/tailrecursion/javelin
[2]: https://github.com/tailrecursion/castra
[3]: https://github.com/tailrecursion/cljson
[4]: https://clojars.org/tailrecursion/hoplon
[5]: https://github.com/tailrecursion/hoplon-demos
[6]: doc/Design.md
[7]: #page-declaration
[9]: #getting-started

[11]: https://github.com/tailrecursion/boot
[12]: http://en.wikipedia.org/wiki/S-expression
[13]: https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/HTML5/HTML5_element_list
[14]: https://github.com/tailrecursion/javelin
[15]: https://github.com/tailrecursion/hoplon/blob/master/src/tailrecursion/hoplon.cljs
[16]: #thing-looper
[17]: http://clojuredocs.org/clojure_core/clojure.core/munge
[18]: https://github.com/weavejester/hiccup
[19]: https://github.com/tailrecursion/hoplon/blob/master/doc/Design.md
