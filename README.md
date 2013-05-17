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

### License

Copyright © 2012 The Tailrecursion Collective

Distributed under the Eclipse Public License, the same as Clojure.
