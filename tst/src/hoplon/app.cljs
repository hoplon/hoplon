(ns+ hoplon.app
  (:page
    "index.html")
  (:require
    [javelin.core :refer [cell cell=]]
    [hoplon.core  :refer :all]))

; (defelem toc
;   [{:keys [toc]} _]
;   (div
;     (h3 "Table of Contents")
;     (div
;       (loop-tpl
;         :bindings [{:keys [num title]} toc]
;         (p (a :href "#" (text "~{num}...~{title}")))))))
;
; (defelem chapter
;   [{:keys [toc title]} kids]
;   (let [chapter? #(= :chapter (:type %))
;         chapters (filter chapter? @toc)
;         num      (inc (count chapters))
;         this-toc {:type :chapter :num num :title title}]
;     (swap! toc conj this-toc)
;     (section
;       (h1
;         (text "Chapter ~{num}: ~{title}"))
;       kids)))

(html
  (head
    (link :rel "stylesheet" :href "app.css")
    (html-meta :charset "UTF-8")
    (base :href "http://hoplon.io/images/")
    (title "Test examples")
    (style "h1 {color:red};} p {color:blue};}"))
  (body
    (noscript "Your browser does not support JavaScript!")
    (noframes "Sorry, your browser does not handle frames!")
    (script "1;")
    (nav
      (menu :type "context" :id "mymenu"
        (menuitem :label "Refresh")))
    (article
      (header
        (h1 "hello world")
        (h2 "something"))
      (h3 "less important")
      (hgroup
        (h4 "getting lower")
        (h5 "quite specific"))
      (h6 "maximum specificity")
      (a :href "http://hoplon.io/" "a link"))
    (hr)
    (abbr :title "Cascading Style Sheet" "CSS")
    (aside
      (address "123 Best Street, Nowhere")
      "Some text" (b "with cool") "styling")
    (figure
      (img
        :src "logos/hoplon-logo.png"
        :usemap "#logomap")
      (figcaption "The logo"))
    (html-map
      :name "logomap"
      (area
        :shape "rect"
        :coords "0,0,100,100"
        :href "http://hoplon.io"
        :alt "click me"))
    (audio :controls true)
    (html-object :width "400" :height "400" :data "helloworld.swf"
      (param :name "autoplay" :value "true"))
    (br)
    (bdo
      :dir "rtl"
      "Right to " (bdi "left"))
    (section
      (blockquote
        :cite "http://hoplon.io"
        "Hoplon is a set of "
        (strong "Clojure and ClojureScript")
        " libraries that pave over the web's "
        (span "idiosyncrasies and present a simpler")
        " way to design and "
        (small "build single-page")
        " web applications. "
        (samp "Learn more on our wiki.")))
    (pre
      "Text in a pre element
      is displayed in a fixed-width
      font, and it preserves
      both      spaces and
      line breaks")
    (button :type "button" "click me")
    (canvas)
    (table
      (caption "Monthly savings")
      (colgroup
        (col)
        (col))
      (tr
        (th "Month")
        (th "Savings"))
      (tr
        (td "January")
        (td "$100")))
    (table
      (thead
        (tr
         (th "Month")
         (th "Savings")))
      (tfoot
        (tr)
        (td "Sum")
        (td "$180"))
      (tbody
        (tr
          (td "January")
          (td "$100"))))
    (p  "Can I get "
        "?")
    (cite "cite")
    (code "(= (+ 1 1) 2)")
    (input :list "numbers")
    (datalist :id "numbers"
      (option :value "one")
      (option :value "two")
      (option :value "three"))
    (label :for "some-input")
    (input :id "some-input")
    (textarea)
    (keygen :name "security")
    (sup "foo")
    (sub "bar")
    (dl
      (dt "Coffee")
      (dd "Black hot drink")
      (dt "Milk")
      (dd "White cold drink"))
    (main
      (p "My favorite color is " (del "blue") (ins "red") "!")
      (details
        (summary "Copyright 1999-2014.")
        (p " - by Refsnes Data. All Rights Reserved.")
        (p "All content and " (s "graphics" ) " on this web site are the property of the company Refsnes Data."))
      (div (dfn "HTML") " is the " (em "standard markup") "language for creating web pages.")
      (p "He named his car " (i "The lightning") ", because it was very fast.")
      (p  "Do not "
          (q "forget to buy ")
          (mark "milk")
          " today."))
    (ruby
      "漢"
      (rp "(")
      (rt "ㄏㄢˋ")
      (rtc "San Francisco")
      (rp ")"))
    (meter :value "2" :min "0" :max "10" "2 out of 10")
    (progress :value "22" :max "100")
    (embed :src "foo.swf")
    (video :width "320" :height "240" :controls true
      (source :src "movie.mp4" :type "video/mp4")
      (source :src "movie.ogg" :type "video/ogg")
      (track :src "subtitles_no.vtt" :kind "subtitles" :srclang "no" :label "Norwegian")
      "Your browser does not support the video tag.")
    (p  "To learn AJAX, you must be familiar with the XML"
        (wbr "Http")
        "Request Object.")
    (dialog :open true
      (p "Greetings, one and all!"))
    (form
      (fieldset
        (legend "Personalia:")
        "Name: " (input :type "text")
        "Email: " (input :type "text")
        "Date of birth: " (input :type "text")))
    (iframe :src "http://hoplon.io")
    (ol
      (li "Coffee")
      (li "Tea")
      (li "Milk"))
    (ul
      (li "Coffee")
      (li "Tea")
      (li "Milk"))
    (html-var "Variable")
    (p "New " (u "Products"))
    (ul
      (li
        (data :value "3967381398" "Mini Ketchup"))
      (li
        (data :value "3967381399" "Jumbo Ketchup"))
      (li
        (data :value "3967381400" "Mega Jumbo Ketchup")))
    (p "We open at " (html-time "10:00") " every morning.")
    (select
      (optgroup :label "Swedish Cars"
        (option :value "volvo" "Volvo")
        (option :value "saab" "Saab"))

      (optgroup :label "German Cars"
        (option :value "mercedes" "Mercedes")
        (option :value "audi" "Audi")))
    (let
      [ a (cell 10)
        b (cell 20)]
      (form
        (input :id "a" :value a :input #(reset! a (int @%)))
        "+"
        (input :id "b" :value b :input #(reset! b (int @%)))
        "="
        (output :for "a b" :value (cell= (+ a b)))))
    (footer
      (p "Press " (kbd "cmd")))))
