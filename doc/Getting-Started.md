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

HTML and JavaScript files will be created in the _resources/public_ directory.

### Review

The only unusual thing here is the `<script>` tag in _index.html_.
