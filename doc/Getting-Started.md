# Getting Started

Hoplon projects require [leiningen](https://raw.github.com/technomancy/leiningen/stable/bin/lein).
You can create a skeleton project using the leiningen Hoplon template:

```bash
$ lein new hoplon hello
```

### Compiling The Application

The Hoplon leiningen plugin compiles source HTML files and ClojureScript libraries
into a HTML and JavaScript application. The compiler is provided as a leiningen
plugin. In the project directory run the compiler:

```bash
$ lein hoplon
```

There is also a watcher-based operating mode that will continuously recompile the
application as source files are modified:

```bash
$ lein hoplon auto
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

* Attribute nodes are represented as a map of keyword keys to string values
  immediately following the tag name.
* Text nodes are represented as strings.
* Parentheses may be omitted around elements which have no children or attributes.

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

This flexibility makes it easier to edit source files in whichever
editor or IDE is preferred, although editing mixed-syntax files is
always more difficult than editing pure ClojureScript or HTML.

### ClojureScript CSS Literal Syntax

When editing HTML as s-expressions the compiler will also parse `<style>`
elements containing ClojureScript CSS definition syntax:

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
Each of the HTML5 elements is defined, i.e. `a`, `div`, `span`, `p`, etc.
The ClojureScript element node type has the following properties:

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
<html>
  <head>
    <title>Hello Functions</title>
  </head>
  <body>
    <script type="text/hoplon">
      (ns hello.func2)
      
      (defn fancyitem [heading body]
        (li
          (h2 heading)
          (p body)))
    </script>
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

## Reactive Attributes

An example of how macros can be used to advantage is the `reactive-attributes`
macro that ships with Hoplon.
