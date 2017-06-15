# Changes

## 7.0.0

#### Improved

* Elements that are not passed any cell children render faster [4c9c2a6](4c9c2a6)

#### Tasks

* `ns+`: Still provided, but no longer called by the `hoplon` task and marked as deprecated.

#### Fixed

* `nil` children passed as arguments to element constructors are ignored [#160][160]

[160]: https://github.com/hoplon/hoplon/pull/160
[4c9c2a6]: https://github.com/hoplon/hoplon/commit/4c9c2a65ef94de88e10827acc84fd1b43e034305
