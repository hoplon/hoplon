# hoplon

## 4.0.3

*Tue Nov 19 14:29:47 EST 2013*

* Guard calls to `appendChild` method with try/catch block when building DOM
  in ie8. **This does not affect other browsers.**

## 4.0.2

*Mon Nov 18 17:07:59 EST 2013*

* Use currying to split page expression into separate function applications to
  reduce imapct on JS stack when building page DOM. Safari especially couldn't
  handle "broad" and moderately deep stack allocation and will freeze without
  the currying.

This involves a slight change to how the page expression is evaluated, so:

* Macros in page expression can only take string arguments.
* Page markup must consist of DOM elements, custom elements, or functions that
  are curried (i.e. `f` such that `(f x y z)` is equivalent `(((f x) y) z)`).

## 4.0.1

*Fri Nov 15 13:04:01 EST 2013*

* Add special `:css` attribute to set styles on elements
* Fix issue with setting/unsetting attributes via `:do-attr`
* Remove `<head>` merging from prerender task since `<head>` is no longer
  dynamically created.

## 4.0.0

*Wed Nov 13 15:51:04 EST 2013*

* The `html`, `head`, and `body` elements can't be reliably created at runtime
  so head contents are inserted verbatim and not evaluated anymore (browser
  compatibility)
* Attributes are now set using jQuery (browser compatibility)
* Various shims and workarounds for ie8 (browser compatibility)

## 3.3.0

*Fri Nov  8 10:40:24 EST 2013*

* fix issue with nonexistent `console.log` causing NPE in ie8
* add `on-append!` function to allow overriding appendChild and setAttribute
  behavior in user-defined types
* add `readonly` to list of attributes that need to be set with `setAttribute`

## 3.2.1

*Wed Nov  6 13:00:12 EST 2013*

* add `;;{{` `;;}}` multiline text escaping
* add `:prepend-head` clause to page declaration to add tags to head of html
  output document directly at compile time.
* various bugfixes

## 3.2.0

*Fri Nov  1 15:46:57 EDT 2013*

* add `defelem` macro
* merge CSS classes instead of replacing: `((div :class "foo") :class "bar")`
  &rarr; `(div :class "foo bar")`

## 3.1.0

*Fri Nov  1 02:26:31 EDT 2013*

* add compilation for .hl files that are libraries and not pages
* fix issues with watch-mode operation where deleting files caused NPE

## 3.0.4

*Thu Oct 31 04:16:47 EDT 2013*

* prerender task now runs when hoplon task runs

## 3.0.3

*Wed Oct 30 16:36:22 EDT 2013*

* back out normlization code
* remove `with-frp` macro and `thing-looper` function
* add `val-id` and `by-id` functions to fetch element/input-value by id
* add `loop-tpl` macro to replace `thing-looper`
* roll reactive attributes into Hoplon core, extending `IFn` to `Element` type
* reactive attributes now take a single argument, no longer wrap attr values
  in vectors
* wrap cljs expressions in attr value string with `{{ }}` in html syntax source
  files to remove ambiguous cases
* add `prerender` task to populate html in output file by phantomjs scraping
* add `html2cljs` tasks to convert between source file syntaxes

## 3.0.2

*Fri Oct 25 14:52:49 EDT 2013*

* fix bug in normalization code

## 3.0.1

*Fri Oct 25 12:32:54 EDT 2013*

* fix issue where html and head tags not being "normalized"

## 3.0.0

*Fri Oct 25 05:07:03 EDT 2013*

* better perf all around, many changes.

## 2.1.0

*Wed Oct 23 03:36:35 EDT 2013*

* Add auto-gensym ids in :loop bindings

## 2.0.0

*Tue Oct 22 14:14:34 EDT 2013*

* Hoplon source files (.hl extension) now use the `page` form instead of `ns`
  to declare the output file and require/refer names into page namespace.

## 1.1.4

*Mon Oct 21 16:44:03 EDT 2013*

* remove `add-hoplon-uses` code from compiler in favor of
  `#'tailrecursion.javelin/refer-all` in user code.
* update javelin dependency

## 1.1.3

*Sun Oct 20 16:29:51 EDT 2013*

* update javelin dependency
* add html2cljs task

## 1.1.2

*Sat Oct 19 18:40:40 EDT 2013*

* Update javelin dependency.
