(ns pluto.playground.components.source
  "A source component based on CodeMirror"
  (:require [reagent.core :as reagent]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.parinfer
            cljsjs.parinfer-codemirror))

(defn viewer [{:keys [on-change content]}]
  (let [cm (atom nil)]
    (reagent/create-class
      {:component-did-mount
       (fn [this]
         (let [el (js/CodeMirror. (reagent/dom-node this)
                    (clj->js
                      {:lineNumbers     true
                       :viewportMargin  js/Infinity
                       :matchBrackets   true
                       :styleActiveLine true
                       :autofocus       true
                       :mode            "clojure"}))]
           (js/parinferCodeMirror.init el)
           (.setValue el @content)
           (when on-change
             (.on el "change" #(on-change (.getValue el))))
           (reset! cm el)))
       :component-did-update
       (fn [this old-argv]
         (when-not (= @content (.getValue @cm))
           (.setValue @cm @content)
           ;; reset the cursor to the end of the text, if the text was changed externally
           (let [last-line (.lastLine @cm)
                 last-ch (count (.getLine @cm last-line))]
             (.setCursor @cm last-line last-ch))))
       :reagent-render
       (fn [_]
         @content
         [:div {:class "codemirror"}])})))
