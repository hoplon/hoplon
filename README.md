<a href="http://hoplon.io/">
  <img src="http://hoplon.io/images/logos/hoplon-logo.png" alt="Hoplon Logo" title="Hoplon" align="right" width="225px" />
</a>

# Hoplon [![clojars][8]][9]  [![build status][14]][15]  [![road map][11]][16] [![Backers on Open Collective][17]](#backers)[![Sponsors on Open Collective][18]](#sponsors)

Hoplon is a set of tools and libraries for making web applications.

Hoplon provides a compiler for web application frontend development, and includes
the following libraries as dependencies to complete the stack:

* [Javelin][1]: a spreadsheet-like dataflow library for managing client
  state. Hoplon tightly integrates with Javelin to reactively bind DOM
  elements to the underlying Javelin cell graph.
* [Castra][2]: a full-featured RPC library for Clojure and
  ClojureScript, providing the serverside environment. (optional)

### Quickstart

Install [Boot](http://boot-clj.com) and then generate a starter project with:

    boot -d boot/new new -t hoplon -n hoplon-starter-project

### Example

`src/pages/index.cljs`:

```clojure
(page "index.html")

(ns ^{:hoplon/page "index.html"} pages.index
  (:require [hoplon.core  :as h :refer [div ul li html head title body h1 span p button text]]
            [javelin.core :as j :refer [cell cell=]]
            [hoplon.jquery]))

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

### Browser Support

Hoplon has been thoroughly tested on desktop and mobile devices against the
following browsers:

[![Backers on Open Collective](https://opencollective.com/hoplon/backers/badge.svg)](#backers) [![Sponsors on Open Collective](https://opencollective.com/hoplon/sponsors/badge.svg)](#sponsors) ![IEdge](https://img.shields.io/badge/IEdge-10%2B-blue.svg) ![Firefox](https://img.shields.io/badge/Firefox-14%2B-orange.svg) ![Safari](https://img.shields.io/badge/Safari-5%2B-blue.svg)
![Chrome](https://img.shields.io/badge/Chrome-26%2B-yellow.svg) ![Opera](https://img.shields.io/badge/Opera-11%2B-red.svg)
![Android](https://img.shields.io/badge/Android-4%2B-green.svg)

>Note that the `object` element is not implemented for IE 8, and that older
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

## Contributors

This project exists thanks to all the people who contribute. 
<a href="https://github.com/hoplon/hoplon/graphs/contributors"><img src="https://opencollective.com/hoplon/contributors.svg?width=890&button=false" /></a>


## Backers

Thank you to all our backers! 🙏 [[Become a backer](https://opencollective.com/hoplon#backer)]

<a href="https://opencollective.com/hoplon#backers" target="_blank"><img src="https://opencollective.com/hoplon/backers.svg?width=890"></a>


## Sponsors

Support this project by becoming a sponsor. Your logo will show up here with a link to your website. [[Become a sponsor](https://opencollective.com/hoplon#sponsor)]

<a href="https://opencollective.com/hoplon/sponsor/0/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/1/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/2/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/3/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/4/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/5/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/6/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/7/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/8/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/hoplon/sponsor/9/website" target="_blank"><img src="https://opencollective.com/hoplon/sponsor/9/avatar.svg"></a>



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
[8]: https://img.shields.io/clojars/v/hoplon.svg
[9]: https://clojars.org/hoplon
[10]: http://en.wikipedia.org/wiki/Deferent_and_epicycle
[11]: https://img.shields.io/badge/road%20map-7.3-lightgrey.svg
[12]: https://badge.waffle.io/hoplon/hoplon.png?label=ready&title=Ready
[13]: https://waffle.io/hoplon/hoplon
[14]: https://travis-ci.org/hoplon/hoplon.svg?branch=master
[15]: https://travis-ci.org/hoplon/hoplon
[16]: https://github.com/hoplon/hoplon/milestones?direction=desc&sort=completeness&state=open
[17]: https://opencollective.com/XX/backers/badge.svg
[18]: https://opencollective.com/XX/sponsors/badge.svg
 