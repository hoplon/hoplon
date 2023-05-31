<a href="http://hoplon.io/">
  <img src="http://hoplon.io/images/logos/hoplon-logo.png" alt="Hoplon Logo" title="Hoplon" align="right" width="225px" />
</a>

# Hoplon
[![clojars](https://img.shields.io/clojars/v/hoplon.svg)](https://clojars.org/hoplon)
[![snapshot status](https://img.shields.io/clojars/vpre/hoplon.svg)](https://clojars.org/hoplon)

[![road map](https://img.shields.io/badge/road%20map-7.3-lightgrey.svg)](https://github.com/hoplon/hoplon/milestones?direction=desc&sort=completeness&state=open)
[![Backers on Open Collective][5]](#backers)
[![Sponsors on Open Collective][6]](#sponsors)
[![cljdoc badge](https://cljdoc.org/badge/hoplon/hoplon)](https://cljdoc.org/d/hoplon/hoplon)

Hoplon is a ClojureScript library that unify some of the web platform's 
idiosyncrasies and present a fun way to design and build single-page web
applications.

Hoplon tightly integrates with Javelin to reactively bind DOM elements to the
underlying [Javelin][1] cell graph.

### Quickstart

Install [deps-new](https://github.com/seancorfield/deps-new) if you haven't already:

    clojure -Ttools install io.github.seancorfield/deps-new '{:git/tag "v0.5.2"}' :as new

And then generate a starter project with:

    clojure -Sdeps '{:deps {io.github.hoplon/project-template {:git/tag "v0.3.0" :git/sha "ed6ce5f"}}}' -Tnew create :template hoplon/hoplon :name your/app-name

### Example
A small bit of Hoplon:

```clojure
(ns view.index
  (:require
    [hoplon.core  :as h]
    [hoplon.goog]
    [javelin.core :as j]))

(defn my-list [& items]
  (h/div :class "my-list"
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

![IEdge](https://img.shields.io/badge/IEdge-10%2B-blue.svg)
![Firefox](https://img.shields.io/badge/Firefox-14%2B-orange.svg)
![Safari](https://img.shields.io/badge/Safari-5%2B-blue.svg)
![Chrome](https://img.shields.io/badge/Chrome-26%2B-yellow.svg)
![Opera](https://img.shields.io/badge/Opera-11%2B-red.svg)
![Android](https://img.shields.io/badge/Android-4%2B-green.svg)

### Documentation

* [https://hoplon.io][4]
* [Design Document][3] (early version)
* [The Wiki](https://github.com/hoplon/hoplon/wiki)

### Demos

* [Hoplon demo applications repository][2]

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
[2]: https://github.com/hoplon/demos
[3]: https://github.com/hoplon/hoplon/blob/cf9d2d1e806d36d098ae1def3b130df2bcd69e55/Design.md
[4]: https://hoplon.io/
[5]: https://opencollective.com/XX/backers/badge.svg
[6]: https://opencollective.com/XX/sponsors/badge.svg
