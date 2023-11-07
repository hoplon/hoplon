# Hoplon Changelog

## Unreleased

## 7.4.0
- Make specs and spec validation optional when building the application
- BREAKING: users that are using spec validation and call `(hoplon.core/spec!)`
  now need to call `(hoplon.googl-spec/spec!)` or `(hoplon.jquery-spec/spec!)`
  depending on the provider they use. The default provider if you don't have a
  explicit require on `hoplon.jquery` or `hoplon.goog` is jquery.
- BREAKING: remove broken attributes on goog provider. The following attributes
  were removed:
  - `:slide-toggle`
  - `:focus`
  - `:select`
  - `:focus-select`
  - `:html`
  - `:scroll-to`
- goog implementation is considered deprecated at this point considering that
  Closure Library is in [maintenance mode](https://github.com/google/closure-library/issues/1214)

## 7.3.5
- Update dependencies

## 7.3.4
- `with-init!` execute the fn immediately if the page has already loaded (fixes #257)
- Fix singletons appending instead of replacing children (fixes #264)

#### Library developers
- Use Element.matches() instead of Element.webkitMatchesSelector() in tests (fixes #272)

## 7.3.3
- Derefing an event now works when using `hoplon.goog` attribute provider

## 7.3.2
- Move the cljsjs.jquery dependency back to regular deps
- More :no-doc metadata to fix building on cljdoc

#### Library developers
- Fix lint warnings from clj-kondo

## 7.3.1
- Add some :no-doc metadata to fix building on cljdoc
- Fix scm link so cljdoc can find the project on github

## 7.3.0
- clj-kondo config to fix errors displayed by clojure-lsp
- `with-animation-frame` macro
- Move the cljsjs.jquery dependency to test deps

#### Library developers
- Use Github Actions to test, release snapshots and versions
- Move tooling from boot to tools deps
- Test setup using chrome webdriver
- npm setup used for testing

## 7.2.0

#### Bug Fixes
  - `defattr` spec was not being used, improved spec

#### Refactored
  - Avoid Protocol Override
  - Rename `INode` to `IHoplonNode` [da849e0](https://github.com/hoplon/hoplon/pull/216/commits/da849e01b3a954b1bd2fa7d82c050fe630317a70)
  - Reorganized `hoplon.core`
  - mark `-do!` and `-on!` as private
  - renamed `-attr!` to `-attribute!`
  - `dispatcher` now used by all multi-methods [9561df6](https://github.com/hoplon/hoplon/pull/216/commits/9561df6ebfdf55e95d196862ed0af7360a0d8840)
  - Improved runtime specs
  - Removed `when-dom` for `on!` events

#### Added
  - `dispatcher` helper function [ba36c95](https://github.com/hoplon/hoplon/pull/216/commits/ba36c95a724f03afc75b2ceb29bda5c313803286)
  - `elem!` multi-method [9561df6](https://github.com/hoplon/hoplon/pull/216/commits/9561df6ebfdf55e95d196862ed0af7360a0d8840)
  - Managed Versions via `boot-semver`

#### Removed
  - `boot-hoplon` warning
  - `adzerk/bootlaces` tasks
  - Experimental features moved to `hoplon.experimental`
  - Design.md file [design.md](https://github.com/hoplon/hoplon/wiki/HLisp)
  - `def-values` [f9a4aab](https://github.com/hoplon/hoplon/pull/216/commits/f9a4aab97c69a841ff575f8cb73bfd91f9d9272a)
  - `do-def` [836cc95](https://github.com/hoplon/hoplon/pull/216/commits/836cc9540cc9b0222cd15ee50ab3b7057a0d6b17)
  - `subs` [27c984f](https://github.com/hoplon/hoplon/pull/216/commits/27c984f83eace8c6d1072350ab3cce73b0d025dc)
  - `name` [27c984f](https://github.com/hoplon/hoplon/pull/216/commits/27c984f83eace8c6d1072350ab3cce73b0d025dc)
  - `page-load` [778eb91](https://github.com/hoplon/hoplon/pull/216/commits/778eb9112bd3084b6f890a471bc4d1e0d3193ae9)

#### Moved
  - `bust-cache` [4f3638e](https://github.com/hoplon/hoplon/pull/216/commits/4f3638e61bd979983c35865e0840027385dd6233)
  - `cache-key` [4f3638e](https://github.com/hoplon/hoplon/pull/216/commits/4f3638e61bd979983c35865e0840027385dd6233)
  - `map-bind-keys` [bc02bf5](https://github.com/hoplon/hoplon/pull/216/commits/bc02bf55751a31aacf76c3b43c54ccecbef1159e)
  - `static-elements` [920dbc6](https://github.com/hoplon/hoplon/pull/216/commits/920dbc66d09ae7ef47c32f9477b8347ca7d76135)
  - `route-cell` [0a539f0](https://github.com/hoplon/hoplon/pull/216/commits/0a539f056187918cbd6237392c9495cb3d0f7179)
  - `ns+` [8362ace](https://github.com/hoplon/hoplon/pull/216/commits/8362ace584fb576b06fab8c3d33a60318d7432ca)

#### Tests
  - `text` macro tests for interpolation

## 7.1.0

#### Improved

  - Upgrade to Clojure 1.9
  - Upgrade to ClojureScript 1.9.946
  - Upgrade Latest Dependencies
  - Test Coverage

#### Added

  - Spec support for Macros
  - Spec support for Runtime Attributes
  - Docstring and Pre/Post Conditions to `defelem`

#### Removed

  - IE8 Support

## 7.0.0

#### Improved

  - Elements that are not passed any cell children render faster [4c9c2a6](https://github.com/hoplon/hoplon/commit/4c9c2a65ef94de88e10827acc84fd1b43e034305)

#### Tasks

  - `ns+`: Still provided, but no longer called by the `hoplon` task and marked as deprecated.

#### Fixed

  - `nil` children passed as arguments to element constructors are ignored [#160](https://github.com/hoplon/hoplon/pull/160)
