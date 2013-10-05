# Getting Started

Hoplon applications are be built using the 
(http://github.com/tailrecursion/boot)[boot]
build tool. The following `boot.clj` file is a good starting point:

```clojure
{:project       my-hoplon-project
 :version       "0.1.0-SNAPSHOT"
 :dependencies  [[tailrecursion/boot.task "0.1.0-SNAPSHOT"]
                 [tailrecursion/hoplon "1.1.0-SNAPSHOT"]]
 :require-tasks #{[tailrecursion.boot.task :refer :all]}
 :src-paths     #{"src/hoplon" "src/clj" "src/cljs"}
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

### Compiler Source and Output Directories

By default the compiler looks in certain directories for source files. Since web
applications are organized around URLs, the HTML source files that make up the 
"pages" of the application are expected to be in a certain directory in the project.
Subdirectories in this HTML source directory are reproduced in the output directory
so that the pages end up at the right URLs when the application is deployed.

* _src/html_ HTML source files and subdirectories.
* _src/cljs_ ClojureScript source files for namespaces which can be required in the
  HTML files.
* _src/static_ Static content. Files and subdirectories here are overlayed on the
  output directory.
* _resources/public_ HTML and JavaScript output files, subdirectories, and static
  content.

### Library And Package Management

Hoplon projects can have dependencies in the _project.clj_ file. These dependencies
can be any of the following:

* Clojure namespaces (ClojureScript macros are written in Clojure).
* ClojureScript namespaces to be used in the project.
* Raw JavaScript source files to be prepended (in dependency order) to the _main.js_
  output file.
* Google Closure Compiler ready JavaScript source and extern files.

Note that JavaScript dependency jar files must be prepared a certain way, described
[here](#).

## Hello World

The simplest example application looks almost exactly like a standard HTML web
page, with the exception of an unfamiliar script tag containing a namespace
declaration. All HTML source files in a Hoplon application must declare a namespace.
This is because the HTML contained in the document body is going to be _evaluated_
as ClojureScript in the browser.

_src/html/index.html_:

```html
<html>
  <head></head>
  <body>
    <script type="text/hoplon">
      (ns hello.index)
    </script>   
    <h1 id="main" style="color:red">Hello world</h1>
  </body>
</html>
```

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

_src/html/sexp.cljs_

```clojure
(html
  head
  (body
    (ns hello.index)
    (h1 {:id "main" :style "color:red"} "Hello world")))
```

When the application is compiled the output file _resources/public/sexp.html_
is produced.

The ClojureScript HTML syntax follows the following conventions:

* An element is represented as a list enclosed in parentheses.
* The first item in the list must be the element's tag name.
* The second item may be an attribute map with keyword keys if the element
  has attribute nodes.
* The rest of the items are the element's children and may be text or element
  nodes.
* Text nodes are represented as strings or `($text "Value")`.
* Parentheses may be omitted around elements which have no children or
  attributes.

Note that the script element has been removed in the sexp version. The script
element in the HTML version serves simply to splice the lisp expressions it
contains into the surrounding HTML markup. This is necessary when working in
HTML markup because ClojureScript source code is not strictly s-expressions; it
is made up of lists, maps, vectors, reader macros, etc., and names which are
valid in ClojureScript may contain characters which would crash a sane HTML
parser (the function `clj->js`, for example, which cannot be represented in
HTML markup as `<clj->js/>`).

### Document Structure

In order to facilitate the HTML-as-ClojureScript s-expression representation
the compiler will reorder expressions such that the above program can also be
represented in a format that works well with ClojureScript editors and tools:

```clojure
(ns hello.index)

; definitions and initialization expressions can go here

(html
  head
  (body
    (h1 {:id "main" :style "color:red"} "Hello world")))
```

Of course, the compiler will also accept "out-of-body" script tags when
parsing HTML syntax, too:

```html
<script type="text/hoplon">
  (ns hello.index)
  
  ; definitions and initialization expressions can go here
</script>

<html>
  <head></head>
  <body>
    <h1 id="main" style="color:red">Hello world</h1>
  </body>
</html>
```

In general, Hoplon programs can be represented equivalently in either
HTML or ClojureScript syntax. This is an important point for designers
and developers who rely on development tools to get the maximum level
of productivity.

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

_src/html/func.cljs_

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

When _resources/public/func.html_ is loaded the list items, headings, and
paragraphs will be seen in the resulting HTML. As always, the same page can
be represented as HTML markup:

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

An example of how macros can be used to advantage is the `reactive-attributes`
macro that ships with Hoplon. The `tailrecursion.hoplon.reactive` library
ties FRP data structures from [Javelin](http://github.com/tailrecursion/javelin)
to the DOM. Consider the following program:

_src/html/react1.html_

```html
<script type="text/hoplon">
  (ns hello.react1
    (:require-macros
      [tailrecursion.javelin.macros   :refer [cell]]
      [tailrecursion.hoplon.macros    :refer [reactive-attributes]])
    (:require
      [tailrecursion.javelin          :as j]
      [tailrecursion.hoplon.util      :as u]
      [tailrecursion.hoplon.reactive  :as r]))
  
  (def clicks (cell 0))
</script>

<html>
  <head>
    <title>Reactive Attributes: Example 1</title>
  </head>
  <body>
    <reactive-attributes>
      <h1 on-click='#(swap! clicks inc)'>
        Click Me
      </h1>
      <ul>
        <li>
          You clicked
          <span do-text='(format " %s %s " clicks (u/pluralize "time" clicks))'/>
          so far.
        </li>
      </ul>
    </reactive-attributes>
  </body>
</html>
```

Clicking on the "click me" element causes the span to update, its text
reflecting the number of times the user has clicked so far.


### Reactive Library

The reactive library, `tailrecursion.hoplon.reactive`, provides a number
of DOM manipulation functions that can be 
