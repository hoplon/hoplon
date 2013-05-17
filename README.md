# Hoplon

Hoplon is a set of tools and libraries for making web application front ends. It
has no server side component but can be used with any back end available.

### Example

```html
<html>
  <head>
    <title>example page</title>
  </head>
  <body>
    <script type="text/hoplon">
      (ns example.index)
      
      (defn myfn [x y]
        (div {:class "foo"}
          (ul (li x)
              (li y))))
    </script>
    
    <h1>Hello, Hoplon</h1>
    
    <myfn>
      <div>first thing</div>
      <div>second thing</div>
    </myfn>
  </body>
</html>
```

### Dependency

Artifacts are [published on Clojars](https://clojars.org/tailrecursion/hoplon). 
Put this in your _project.clj_:

```clojure
(defproject my-project "0.1.0-SNAPSHOT"
  :plugins      [[thinkminimo/hoplon "0.1.0-SNAPSHOT"]]
  :dependencies [[thinkminimo/hoplon "0.1.0-SNAPSHOT"]])
```

### Demos and Examples

* here
* and here

## Documentation

* [Getting Started](https://github.com/tailrecursion/hoplon/wiki/Getting-Started)
* [Configuration](https://github.com/tailrecursion/hoplon/wiki/Configuration)
* [API Documentation](https://github.com/tailrecursion/hoplon/wiki/API-Documentation)

## License

```
Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```
