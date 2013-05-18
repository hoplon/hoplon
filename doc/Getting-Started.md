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

Source file _src/html/index.html_:

```html
<html>
  <head>
    <title>Hello</title>
  </head>
  <body>
    <script type="text/hoplon">
      (ns hello.index)
    </script>
    
    <h1>Hello world</h1>
  </body>
</html>
```
