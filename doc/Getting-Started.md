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

### Compiler Output Directory

The HTML and JavaScript files are created in the _resources/public_ directory by
default.

## Hello World

The simplest example application looks almost exactly like a standard HTML web
page, with the exception of an unfamiliar script tag containing a namespace
declaration. All HTML source files in a Hoplon application must declare a namespace.
This is because the HTML contained in the document body is going to be _evaluated_
as ClojureScript in the browser.
