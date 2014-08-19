# twitter.bootstrap

A [Hoplon][hoplon] wrapper for [Twitter's Bootstrap framework][2].

## Dependency

[Artifacts published on Clojars.][1]

## Usage

Simple example page using it in a page:

```clojure
(page "index.html"
  (:require [hoplon.twitter.bootstrap :refer [container]]))

(html
  (head)
  (body
    (container
      (div :col {:sm 10} :offset {:sm 2}
        (h1 "Hello World")
        (p "This is a test!")))))
```

## License

Copyright © 2014, Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure

[hoplon]: http://hoplon.io
[1]: https://clojars.org/io.hoplon/twitter.bootstrap
[2]: https://getbootstrap.com
