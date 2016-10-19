<a href="http://hoplon.io/">
  <img src="http://hoplon.io/images/logos/hoplon-logo.png" alt="Hoplon Logo" title="Hoplon" align="left" width="225px" />
</a>
# Hoplon [![build status][14]][15] ![epicycles][11]

[](dependency)
```clojure
[hoplon "6.0.0-alpha17"] ;; latest release
```
[](/dependency)

Hoplon is a set of tools and libraries for making web applications.

Hoplon provides a compiler for web application frontend development, and includes
the following libraries as dependencies to complete the stack:

* [Javelin][1]: a spreadsheet-like dataflow library for managing client
  state. Hoplon tightly integrates with Javelin to reactively bind DOM
  elements to the underlying Javelin cell graph.
* [Castra][2]: a full-featured RPC library for Clojure and
  ClojureScript, providing the serverside environment.

### Example

```clojure
(page "index.html")

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
    (button :click #(swap! clicks inc) "click me")))
```

Or, equivalently:

```xml
<script type="text/hoplon">
  ;; Page declaration specifies output file path.
  (page "index.html")

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

    <!-- using dataflow to link DOM and Javelin cells -->
    <p><text>You've clicked ~{clicks} times, so far.</text></p>
    <button click="{{ #(swap! clicks inc) }}">click me</button>
  </body>
</html>
```

### Browser Support
Hoplon has been thoroughly tested on desktop and mobile devices against the
following browsers:

* IEdge   8+
* Firefox 14+
* Safari  5+
* Chrome  26+
* Opera   11+
* Android 4.0+

Note that the `object` element is not implemented for IE 8, and that older
browsers that predate HTML 5 elements such as `Audio` and `Video` will not render
them.  Additionally, boot development tasks such as `boot-reload` and
`boot-cljs-repl`, which inject scripts into the browser to function, do not
support IE 8 (which errors when output is written to the console without the
developer tools open).  Testing against these browsers is best done with simple
or advanced optimizations turned on.

### Documentation

* [http://hoplon.io][7]
* [Design Document][6] (early version)
* [The Wiki](https://github.com/hoplon/hoplon/wiki)

### Demos

* [Hoplon demo applications repository][5]

## Hacking

```
# build and install locally
boot develop
```

## License

```
Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```

[1]: https://github.com/hoplon/javelin
[2]: https://github.com/hoplon/castra
[3]: https://github.com/hoplon/cljson
[4]: https://clojars.org/hoplon/hoplon
[5]: https://github.com/hoplon/hoplon-demos
[6]: Design.md
[7]: http://hoplon.io/
[8]: https://clojars.org/hoplon/hoplon/latest-version.svg?bustcache=2
[9]: http://hoplon.github.io/hoplon/
[10]: http://en.wikipedia.org/wiki/Deferent_and_epicycle
[11]: http://img.shields.io/badge/epicycles-0-green.svg?cache=1
[12]: https://badge.waffle.io/hoplon/hoplon.png?label=ready&title=Ready
[13]: https://waffle.io/hoplon/hoplon
[14]: https://travis-ci.org/hoplon/hoplon.svg?branch=master
[15]: https://travis-ci.org/hoplon/hoplon
