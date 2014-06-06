Hoplon Contrib Libraries
========================

This is a collection of useful [Hoplon][1] libraries, packaged to be easily
incorporated into your own applications.

* UI kits
* Widget libraries
* CSS frameworks
* jQuery plugins
* Service API clients, and more!

## Usage

Just add them as dependencies to your project, and Hoplon will take care
of the rest! Check the `README` files for API info and usage examples.

## Contribute

Awesome! Pull requests are always welcome!

Some general guidelines to consider when putting together a new contrib
package:

* **Third-party JavaScript, CSS, and resources:** Put Hoplon code (i.e.
  ClojureScript namespaces and the like) in contrib libraries, not 3rd
  party JavaScript, CSS, or resources (images, etc.). If you need to
  include these things please create a [vendor][6] project containing
  the 3rd party code.

* **Project group id:** Use the `io.hoplon` group id, please. When we merge
  your pull request we'll push artifacts to [Clojars][2] for everyone to use.

* **Namespaces:** Please use the `hoplon.your-library` namespace format.

Copyright © 2014 Alan Dipert and Micha Niskin.

[1]: http://hoplon.io
[2]: http://clojars.org
[6]: https://github.com/tailrecursion/hoplon/tree/master/vendor
