# Hoplon

Hoplon is a set of tools and libraries for making web applications.

### Example

```html
<script type="text/hoplon">
  ;; namespace declaration is required
  (ns example.index)
  
  ;; definitions in this file are optional
  (defn myfn [x y]
    (div {:class "foo"}
      (ul (li x)
          (li y))))
</script>
    
<html>
  <head>
    <title>example page</title>
  </head>
  <body>
    <h1>Hello, Hoplon</h1>
    
    <!-- an HTML syntax call to the myfn function -->
    <myfn>
      <div>first thing</div>
      <div>second thing</div>
    </myfn>
  </body>
</html>
```

### Dependency

Artifacts are [published on Clojars](https://clojars.org/tailrecursion/hoplon). 

```clojure
[tailrecursion/hoplon "1.1.0-SNAPSHOT"]
```

### Demos and Examples

* [Hoplon demo applications repository](https://github.com/tailrecursion/hoplon-demos)

## Documentation

* [Getting Started](https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md)
* [Configuration](https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md)
* [API Documentation](https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md)

## License

```
Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```
