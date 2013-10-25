<img src="https://raw.github.com/tailrecursion/hoplon/master/img/Hoplite.jpg">

# Hoplon

Hoplon is a set of tools and libraries for making web applications. Hoplon
provides a compiler for web application frontend development, and includes
the following libraries as dependencies to complete the stack:

* [Javelin][1]: a spreadsheet-like dataflow library for managing client
  state. Hoplon tightly integrates with Javelin to reactively bind DOM
  elements to the underlying Javelin cell graph.
* [Castra][2]: a full-featured RPC library for Clojure and
  ClojureScript, providing the serverside environment.
* [Cljson][3]: an efficient method for transferring Clojure/ClojureScript
  data between client and server. Castra uses cljson as the underlying
  transport protocol.

### Example

```xml
<script type="text/hoplon">
  ;; Page declaration specifies output file path.
  (page index.html)
  
  ;; definitions in this file are optional
  (defn my-list [& items]
    ((div :class "my-list")
       (into ul (map #(li (div :class "my-list-item" %)) items))))

  (def clicks (cell 0))
</script>
    
<html>
  <head>
    <title>example page</title>
  </head>
  <body>
    <with-frp>
      <h1>Hello, Hoplon</h1>
      
      <!-- an HTML syntax call to the my-list function -->
      <my-list>
        <span>first thing</span>
        <span>second thing</span>
      </my-list>

      <!-- using FRP to link DOM and Javelin cells -->
      <p>You've clicked ~{clicks} times, so far.</p>
      <button on-click="#(swap! clicks inc)">click me</button>
    </with-frp>
  </body>
</html>
```

Or, equivalently:

```clojure
(page index.html)

(defn my-list [& items]
  ((div :class "my-list")
     (into ul (map #(li (div :class "my-list-item" %)) items))))

(def clicks (cell 0))

(html
  (head
    (title "example page"))
  (body
    (with-frp
      (h1 "Hello, Hoplon")

      (my-list
        (span "first thing")
        (span "second thing"))

      (p "You've clicked ~{clicks} times, so far.")
      (button :on-click [#(swap! clicks inc)] "click me"))))
```

### Demos

* [Hoplon demo applications repository][5]

### Dependency

Artifacts are published on [Clojars][4]. 

```clojure
[tailrecursion/hoplon "3.0.1"]
```

```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>hoplon</artifactId>
  <version>3.0.1</version>
</dependency>
```

### Documentation

* [Getting Started][6]
* ~~[Configuration][7]~~
* ~~[API Documentation][8]~~
* [Design Document][9]

## Full Example Application

This is an implementation of [TodoMVC][10] using Hoplon (one of the demos
included in the [hoplon-demos][5] repository):

```clojure
;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(page index.html
  (:refer-clojure :exclude [nth])
  (:require
    [tailrecursion.hoplon.util          :refer [nth name pluralize route-cell]]
    [tailrecursion.hoplon.storage-atom  :refer [local-storage]]))

;; internal ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare route state editing)

(defn rm-vec [v i]
  (let [z (- (dec (count v)) i)]
    (cond (neg?   z) v
          (zero?  z) (pop v)
          (pos?   z) (into (subvec v 0 i) (subvec v (inc i))))))

(defn reactive-info [todos i todo]
  [(cell= (= editing i))
   (cell= (:completed todo))
   (cell= (:text      todo))
   (cell= (and (not (empty? (:text todo)))
               (or (= "#/" route)
                   (and (= "#/active" route) (not (:completed todo)))
                   (and (= "#/completed" route) (:completed todo)))))])

;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def state        (local-storage (cell []) ::store))
(def editing      (cell nil))
(def route        (route-cell 100 "#/"))
(def completed    (cell= (filter :completed state)))
(def active       (cell= (remove :completed state)))
(def plural-item  (cell= (pluralize "item" (count active))))
(def loop-todos   (thing-looper state 50 reactive-info :reverse? true))

(def todo         (fn [t]   {:completed false :text t}))
(def destroy!     (fn [i]   (swap! state rm-vec i)))
(def done!        (fn [i v] (swap! state assoc-in [i :completed] v)))
(def clear-done!  (fn [& _] (swap! state #(vec (remove :completed %)))))
(def new!         (fn [t]   (time (when (not (empty? t)) (swap! state conj (todo t))))))
(def all-done!    (fn [v]   (swap! state #(mapv (fn [x] (assoc x :completed v)) %))))
(def editing!     (fn [i v] (reset! editing (if v i nil))))
(def text!        (fn [i v] (if (empty? v) (destroy! i) (swap! state assoc-in [i :text] v))))

;; page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

((html :lang "en") 

   (head
     (title "Hoplon • TodoMVC")
     (meta :charset "utf-8")
     (meta :http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1")
     (link :rel "stylesheet" :href "../assets/base.css"))

   (body
     (with-frp
       (div

         ((section :id "todoapp")
            ((header :id "header")
               (h1 "todos")
               ((form :on-submit [#(do (new! ~:new-todo) (do! ~@:new-todo :value ""))]) 
                  (input :id          "new-todo"
                         :type        "text"
                         :placeholder "What needs to be done?"
                         :autofocus   "autofocus"
                         :on-focusout [#(do! ~@:new-todo :value "")])))

            ((section :id        "main"
                      :do-toggle [(not (and (empty? active) (empty? completed)))])
               (input :id        "toggle-all"
                      :type      "checkbox"
                      :do-attr   [:checked (empty? active)]
                      :on-click  [#(all-done! ~:toggle-all)])
               ((label :for "toggle-all") "Mark all as complete")

               ((ul :id   "todo-list"
                    :loop [loop-todos i edit? done? text show? done# edit#])
                  ((li :style     "display:none;"
                       :do-class  [:completed done? :editing edit?] 
                       :do-toggle [show?]) 

                     ((div :class       "view"
                           :on-dblclick [#(editing! i true)]) 
                        (input :id        done# 
                               :type      "checkbox"
                               :class     "toggle"
                               :do-attr   [:checked done?] 
                               :on-click  [#(done! i ~done#)])
                        (label "~{text}")
                        (button :type      "submit"
                                :class     "destroy"
                                :on-click  [#(destroy! i)]))

                     ((form :on-submit [#(editing! i false)]) 
                        (input :id          edit#
                               :type        "text"
                               :class       "edit"
                               :do-value    [text]
                               :do-focus    [edit?]
                               :on-focusout [#(when @edit? (editing! i false))]
                               :on-change   [#(when @edit? (text! i ~edit#))])))))

            ((footer :id        "footer"
                     :do-toggle [(not (and (empty? active) (empty? completed)))]) 
               ((span :id "todo-count") 
                  (strong "~(count active) ")
                  (span "~{plural-item} left"))

               ((ul :id "filters") 
                  (li ((a :href "#/"          :do-class [:selected (= "#/"          route)]) "All"))
                  (li ((a :href "#/active"    :do-class [:selected (= "#/active"    route)]) "Active"))
                  (li ((a :href "#/completed" :do-class [:selected (= "#/completed" route)]) "Completed")))

               ((button :type      "submit"
                        :id        "clear-completed"
                        :on-click  [#(clear-done!)]) 
                  "Clear completed (~(count completed))")))

         ((footer :id "info") 
            (p "Double-click to edit a todo")
            (p "Part of " ((a :href "http://github.com/tailrecursion/hoplon-demos/") "hoplon-demos")))))))
```

## License

```
Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```

[1]: https://github.com/tailrecursion/javelin
[2]: https://github.com/tailrecursion/castra
[3]: https://github.com/tailrecursion/cljson
[4]: https://clojars.org/tailrecursion/hoplon
[5]: https://github.com/tailrecursion/hoplon-demos
[6]: https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md
[7]: https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md
[8]: https://github.com/tailrecursion/hoplon/blob/master/doc/Getting-Started.md
[9]: https://github.com/tailrecursion/hoplon/blob/master/doc/Design.md
[10]: http://todomvc.com
