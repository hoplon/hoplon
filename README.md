<img src="https://raw.github.com/tailrecursion/hoplon/dox/img/Hoplite.jpg"
alt="tailrecursion/hoplon logo" title="tailrecursion/hoplon logo"
height="300px" align="right"/>

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

```html
<script type="text/hoplon">
  ;; namespace declaration is required
  (ns example.index)
  
  ;; definitions in this file are optional
  (defn myfn [x y]
    (div {:class "foo"}
      (ul (li x)
          (li y))))
</script>
    
<html>
  <head>
    <title>example page</title>
  </head>
  <body>
    <h1>Hello, Hoplon</h1>
    
    <!-- an HTML syntax call to the myfn function -->
    <myfn>
      <div>first thing</div>
      <div>second thing</div>
    </myfn>
  </body>
</html>
```

Or, equivalently:

```clojure
(ns example.index)

(defn myfn [x y]
  (div {:class "foo"}
    (ul (li x)
        (li y))))

(html
  (head
    (title "example page"))
  (body
    (h1 "Hello, Hoplon")
    (myfn
      (div "first thing")
      (div "second thing"))))
```

### Dependency

Artifacts are [published on Clojars][4]. 

```clojure
[tailrecursion/hoplon "1.1.0-SNAPSHOT"]
```

### Demos and Examples

* [Hoplon demo applications repository][5]

## Documentation

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
