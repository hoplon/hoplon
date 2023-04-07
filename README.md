<a href="http://hoplon.io/">
  <img src="http://hoplon.io/images/logos/hoplon-logo.png" alt="Hoplon Logo" title="Hoplon" align="right" width="225px" />
</a>

# Hoplon
[![clojars][8]][9]  [![snapshot status][19]][9] 

[![road map][11]][16] [![Backers on Open Collective][17]](#backers) [![Sponsors on Open Collective][18]](#sponsors)
[![cljdoc badge](https://cljdoc.org/badge/hoplon/hoplon)](https://cljdoc.org/d/hoplon/hoplon)

Hoplon is a set of tools and libraries for making web applications.

Hoplon provides a compiler for web application frontend development, and includes
the following libraries as dependencies to complete the stack:

* [Javelin][1]: a spreadsheet-like dataflow library for managing client
  state. Hoplon tightly integrates with Javelin to reactively bind DOM
  elements to the underlying Javelin cell graph.
* [Castra][2]: a full-featured RPC library for Clojure and
  ClojureScript, providing the serverside environment. (optional)

### Quickstart

Install [deps-new](https://github.com/seancorfield/deps-new) if you haven't already:

    clojure -Ttools install io.github.seancorfield/deps-new '{:git/tag "v0.5.1"}' :as new

And then generate a starter project with:

    clojure -Sdeps '{:deps {io.github.hoplon/project-template {:git/tag "v0.2.0" :git/sha "5a10650"}}}' -Tnew create :template hoplon/hoplon :name your/app-name

### Example
A small bit of Hoplon:

```clojure
(ns view.index
  (:require [hoplon.core  :as h]
            [hoplon.goog]
            [javelin.core :as j]))

(defn my-list [& items]
  (h/div
    :class "my-list"
    (apply h/ul (map #(h/li (h/div :class "my-list-item" %)) items))))

(def clicks (j/cell 0))

(defn hello []
  (h/div
    (h/h1 "Hello, Hoplon")
    (my-list
      (h/span "first thing")
      (h/span "second thing"))
    (h/p (h/text "You've clicked ~{clicks} times, so far."))
    (h/button :click #(swap! clicks inc) "click me")))
```

### Browser Support

Hoplon has been thoroughly tested on desktop and mobile devices against the
following browsers:

![IEdge](https://img.shields.io/badge/IEdge-10%2B-blue.svg) ![Firefox](https://img.shields.io/badge/Firefox-14%2B-orange.svg) ![Safari](https://img.shields.io/badge/Safari-5%2B-blue.svg)
![Chrome](https://img.shields.io/badge/Chrome-26%2B-yellow.svg) ![Opera](https://img.shields.io/badge/Opera-11%2B-red.svg)
![Android](https://img.shields.io/badge/Android-4%2B-green.svg)

### Documentation

* [http://hoplon.io][7]
* [Design Document][6] (early version)
* [The Wiki](https://github.com/hoplon/hoplon/wiki)

### Demos

* [Hoplon demo applications repository][5]

## Developing Hoplon itself

```
# build and install locally
clojure -T:build ci :snapshot true
clojure -T:build install :snapshot true
```

### Testing

This setup will run tests using chrome-webdriver.

#### Setup
```
npm install
npm install -g karma-cli
```
#### Run
```
clojure -T:build test
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
[4]: https://clojars.org/hoplon/hoplon
[5]: https://github.com/hoplon/demos
[6]: https://github.com/hoplon/hoplon/blob/cf9d2d1e806d36d098ae1def3b130df2bcd69e55/Design.md
[7]: http://hoplon.io/
[8]: https://img.shields.io/clojars/v/hoplon.svg
[9]: https://clojars.org/hoplon
[10]: http://en.wikipedia.org/wiki/Deferent_and_epicycle
[11]: https://img.shields.io/badge/road%20map-7.3-lightgrey.svg
[12]: https://badge.waffle.io/hoplon/hoplon.png?label=ready&title=Ready
[13]: https://waffle.io/hoplon/hoplon
[16]: https://github.com/hoplon/hoplon/milestones?direction=desc&sort=completeness&state=open
[17]: https://opencollective.com/XX/backers/badge.svg
[18]: https://opencollective.com/XX/sponsors/badge.svg
[19]: https://img.shields.io/clojars/vpre/hoplon.svg
