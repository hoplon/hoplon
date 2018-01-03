# Hoplon Changelog

## 7.2.0-SNAPSHOT

#### Bug Fixes
  - `defattr` spec was not being used, improved spec

#### Refactored
  - Avoid Protocol Override
  - Rename `INode` to `IHoplonNode` [da849e0](https://github.com/hoplon/hoplon/pull/216/commits/da849e01b3a954b1bd2fa7d82c050fe630317a70)
  - Reorganized `hoplon.core`
  - mark `-do!` and `-on!` as private
  - renamed `loop-tpl*` to `for-tpl*`
  - renamed `-attr!` to `-attribute!`
  - `dispatcher` now used by all multi-methods [9561df6](https://github.com/hoplon/hoplon/pull/216/commits/9561df6ebfdf55e95d196862ed0af7360a0d8840)
  - Improved runtime specs

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
  - `loop-tpl` [ac7799b](https://github.com/hoplon/hoplon/pull/216/commits/ac7799b40bf58c076d759d750e4d864ffd5757b4)
  - `page-load` [778eb91](https://github.com/hoplon/hoplon/pull/216/commits/778eb9112bd3084b6f890a471bc4d1e0d3193ae9)

#### Moved
  - `bust-cache` [4f3638e](https://github.com/hoplon/hoplon/pull/216/commits/4f3638e61bd979983c35865e0840027385dd6233)
  - `cache-key` [4f3638e](https://github.com/hoplon/hoplon/pull/216/commits/4f3638e61bd979983c35865e0840027385dd6233)
  - `parse-e` [7f7f4d1](https://github.com/hoplon/hoplon/pull/216/commits/7f7f4d1f9aa454d782fa41237025c6f18eae8d27)
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
