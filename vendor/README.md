Hoplon Vendor Libraries
=======================

This is a collection of useful third-party JavaScript libraries, packaged for
use with [Hoplon][1].

## Usage

Just add them as dependencies to your project, and Hoplon will take care
of the rest! However, you probably don't want to use them directly---if a
library is included in this section of the repository there's a good chance
that there is a nice Hoplon wrapper for it in the [contrib][6] section, and
you'd probably want to be using that in your application.

## Contribute

Awesome! Pull requests are always welcome!

Some general guidelines to consider when putting together a new vendor package:

* Only one `.inc.js` file per package. The purpose of the package is primarily
  to establish correct dependency relationships in Maven, thereby avoiding
  issues caused by including a JavaScript source file multiple times or by
  including the JavaScript files in the wrong order.

* Put Hoplon wrapper code (i.e. ClojureScript `defelem`s and the like) in
  a [contrib][6] package instead of bundling it here with the JavaScript.
  The contrib package would, of course, have the vendor package as a Maven
  dependency, but this way other ClojureScript libraries can depend on the
  JavaScript vendor package without pulling in the ClojureScript code.

Check out the next section to see how an example package is put together...

## Vendor Package Howto

This howto will use the [jquery.daterangepicker][7] jQuery plugin package
as an example. We'll go through all the steps needed to create a new vendor
package for use in Hoplon projects.

#### 1. Fork Hoplon

The first step is to [fork][2] the Hoplon project on GitHub. You'll add
your new vendor package to your fork of the project, and when everything is
ready you'll make a pull request. Your fork will then be merged with the main
Hoplon project on GitHub, and artifacts will be created and pushed to
[Clojars][3] for everyone to use.

#### 2. Create the directory structure

Once you've forked Hoplon, the next thing you'll want to do is create the
directory structure for your new package. Please create a directory for your
package under the `vendor` subdirectory.

The new package in this howto will be called `jquery.daterangepicker`, and it
will have the following structure:

```
jquery.daterangepicker
├── README.md
├── project.clj
└── src
    ├── jquery.daterangepicker.ext.js
    ├── jquery.daterangepicker.inc.js
    └── jquery.daterangepicker.inc.css
    └── _hoplon
        └── img
            ├── button-sm.png
            └── button-lg.png
```

You'll notice some peculiar conventions going on in there:

* **The `.ext.js` file extension:** This indicates that the file contains a
  [Google Closure externs file][4]. These files are used by the
  [Google Closure compiler][5] when compiling with advanced optimizations.
  Where do you get them? Sometimes you can find them online, pre-made, but
  sometimes you have to create them yourself. Check out the little intro to
  externs below if you need some help creating one.

* **The `.inc.js` file extension:** These files will be prepended to the
  Hoplon application's `main.js` file (in dependency order). This file is
  loaded automatically by Hoplon when the page loads, before any `<script>`
  tags in the application's `<head>`.

* **The `.inc.css` file extension:** These files will be appended to the
  Hoplon application's `main.css` file (in dependency order). This file is
  loaded automatically by Hoplon when the page loads, before any `<link>` or
  `<style>` tags in the application's `<head>`.

* **The `_hoplon` directory:** This directory contains resources that will be
  emitted into the document root of the compiled Hoplon application. The
  `button-sm.png` file, for example, will be accessible in the compiled Hoplon
  application at `img/button-sm.png`.

#### 3. The README.md and project.clj files

You can just copy one of the `README.md` files from another package and edit
it to correspond to your package info.

The example `project.clj` file looks like this:

```clojure
(defproject io.hoplon.vendor/jquery.daterangepicker "0.0.5-0"
  :description  "A jQuery plugin that allows user to select a date range."
  :url          "https://github.com/longbill/jquery-date-range-picker"
  :license      {:name "MIT" :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure       "1.5.1"]
                 [io.hoplon.vendor/jquery   "1.8.2-0"]
                 [io.hoplon.vendor/momentjs "2.6.0-0"]])
```

Again there are a few conventions and details to know about:

* **Name:** Please use the `io.hoplon.vendor` group-id for the project.
  Try to choose a name that is as close to the vendor's name as practical
  without being likely to cause weird name collisions. I prefixed the
  example with `jquery` to indicate that it's a jQuery plugin, the common
  practice in the jQuery community.

* **Version:** The project version here has two parts: the vendor's version
  and the package version. This example is `0.0.5-0`, indicating the plugin
  version `0.0.5` and package version `0`. This makes it possible to release
  a new version of the package while still indicating the version of the
  plugin or JavaScript library.

* **License:** Use the license that the vendor released the plugin or
  library under. In this case it's the MIT license. Make sure you add the
  correct URL, description, and attribution as applicable.

* **Dependencies:** This package depends on the `jquery` and `momentjs`
  packages. Adding them to the `:dependencies` project key will ensure
  that their JavaScript files will be inserted into `main.js` before the
  JavaScript from this package (if `moment.js` were to be inserted after
  `jquery.daterangepicker.js`, for example, you'd see an error at runtime).

#### 4. Test locally

You can install your package locally by doing `lein install`. You can then
add your package as a dependency in a Hoplon test application and try out
your package. When you've verified that everything works, it's time to...

#### 5. Make pull request

You're done! Send us a pull request!

## Quick and Dirty Externs Files

There is some info about [writing your own externs files][8] on the Google
Closure compiler GitHub wiki. However, if you just need to knock together
a simple, quick-and-dirty externs file for your favorite JavaScript library
you can try the following techniques:

**Plain JavaScript objects** are annotated in your externs file like this:

```javascript
google.maps = {};
```

**Object methods** are annotated like this:

```javascript
google.maps.BicyclingLayer = function() {};
```

**Properties and methods on the prototype** are annotated like this:

```javascript
google.maps.BicyclingLayer.prototype.getMap = function() {};
```

And that's about it!

Check out some of the externs files in this section for some more examples
of how it's done. (Also, if you know of any good externs documentation we'd
love to know about it.)

Copyright © 2014 Alan Dipert and Micha Niskin.

[1]: http://hoplon.io
[2]: https://help.github.com/articles/fork-a-repo
[3]: http://clojars.org
[4]: https://developers.google.com/closure/compiler/docs/api-tutorial3
[5]: https://developers.google.com/closure/compiler/
[6]: https://github.com/tailrecursion/hoplon/tree/master/contrib
[7]: https://github.com/tailrecursion/hoplon/tree/master/vendor/jquery.daterangepicker
[8]: https://github.com/google/closure-compiler/wiki/FAQ#how-do-i-write-an-externs-file
