# Getting Started

Get started with Hoplon by making some small, progressively more complex applications.

## Hello World

Create a new project directory.

```bash
$ mkdir -p hello/src/html && cd hello
```

Create a _project.clj_ file.

```clojure
(defproject hello-hoplon "0.1.0-SNAPSHOT"
  :plugins      [[tailrecursion/hoplon "0.1.0-SNAPSHOT"]]
  :dependencies [[tailrecursion/hoplon "0.1.0-SNAPSHOT"]])
```

Create a _src/html/index.html_ file.

```html
<html>
  <head>
    <title>Hello World</title>
  </head>
  <body>
    <script type="text/hoplon">
      (ns hello.index)
    </script>
    <h1>Hello World</h1>
  </body>
</html>
```

Compile application.

```bash
$ lein hoplon
```

Alternatively, watcher-based compilation can be started to automatically
recompile the application whenever source files are modified:

```bash
$ lein hoplon auto
```

HTML and JavaScript files will be created in the _resources/public_ directory.

## S-Expression Syntax

Create a _src/html/sexp.cljs_ file.

```clojure
(html
  (head
    (title "Hello S-Expressions"))
  (body
    (ns hello.sexp)
    (h1 {:style "color:red"} "Hello S-Expressions")))
```

Recompile and check out _resources/public/sexp.html_ in your browser.


