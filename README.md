<img src="https://raw.github.com/tailrecursion/hoplon/master/img/Hoplite.jpg">

# Hoplon

Hoplon is a set of tools and libraries for making web applications. Hoplon
provides a compiler for web application frontend development, and includes
the following libraries as dependencies to complete the stack:

* [Javelin][1]: a spreadsheet-like FRP dataflow library for managing client
  state. Hoplon tightly integrates with Javelin to reactively bind DOM elements
  to the underlying Javelin cell graph.
* [Castra][2]: a full-featured RPC library for Clojure and
  ClojureScript, providing the serverside environment.
* [Cljson][3]: an efficient method for transferring Clojure/ClojureScript data
  between client and server. Castra uses cljson as the underlying transport
  protocol.

### Example

```xml
<script type="text/hoplon">
  ;; namespace declaration is required
  (ns example.index
    (:require tailrecursion.javelin tailrecursion.hoplon.reactive)
    (:require-macros
      [tailrecursion.hoplon.macros  :refer [with-frp]]
      [tailrecursion.javelin.macros :refer [cell]]))
  
  ;; definitions in this file are optional
  (defn my-list [& items]
    (div {:class "my-list"}
      (into ul (map #(li (div {:class "my-list-item"} %)) items))))

  (def clicks (cell 0))
</script>
    
<html>
  <head>
    <title>example page</title>
  </head>
  <body>
    <with-frp>
      <h1>Hello, Hoplon</h1>
      
      <!-- an HTML syntax call to the my-list function -->
      <my-list>
        <span>first thing</span>
        <span>second thing</span>
      </my-list>

      <!-- using FRP to link DOM and Javelin cells -->
      <p>You've clicked ~{clicks} times, so far.</p>
      <button on-click="#(swap! clicks inc)">click me</button>
    </with-frp>
  </body>
</html>
```

Or, equivalently:

```clojure
(ns example.index
  (:require tailrecursion.javelin tailrecursion.hoplon.reactive)
  (:require-macros
    [tailrecursion.hoplon.macros  :refer [with-frp]]
    [tailrecursion.javelin.macros :refer [cell]]))

(defn my-list [& items]
  (div {:class "my-list"}
    (into ul (map #(li (div {:class "my-list-item"} %)) items))))

(def clicks (cell 0))

(html
  (head
    (title "example page"))
  (body
    (with-frp
      (h1 "Hello, Hoplon")

      (my-list
        (span "first thing")
        (span "second thing"))

      (p "You've clicked ~{clicks} times, so far.")
      (button {:on-click [#(swap! clicks inc)]} "click me"))))
```

### Dependency

Artifacts are published on [Clojars][4]. 

```clojure
[tailrecursion/hoplon "1.1.0-SNAPSHOT"]
```

```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>hoplon</artifactId>
  <version>1.1.0-SNAPSHOT</version>
</dependency>
```

### Demos

* [Hoplon demo applications repository][5]

### Documentation

* [Getting Started][6]
* [Configuration][7]
* [API Documentation][8]
* [Design Document][9]

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
[6]: https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md
[7]: https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md
[8]: https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md
[9]: https://github.com/tailrecursion/hoplon/blob/master/doc/Design.md
