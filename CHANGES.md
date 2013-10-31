# hoplon

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
