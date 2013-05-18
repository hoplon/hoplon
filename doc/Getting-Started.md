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
is produced. Notice the syntactic convention:

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

In order to facilitate the HTML-as-ClojureScript s-expression representation
the compiler will reorder expressions such that the above program can also be
represented like this:

```clojure
(ns hello.index)

; definitions and initialization expressions can go here

(html
  head
  (body
    (h1 {:id "main" :style "color:red"} "Hello world")))
```
