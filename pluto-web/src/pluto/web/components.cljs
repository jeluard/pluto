(ns pluto.web.components
  (:require [re-frame.core :as re-frame]))

(defn view [props & content]
  (into [:div props] content))

(defn button [{:keys [on-click]} & content]
  (into [:button {:on-click #(re-frame/dispatch (on-click {}))}] content))

(defn text [props & content]
  (into [:span props] content))

(def all {'view   {:properties {}
                   :value      view
                   :description ""
                   :examples   []}
          'button {:properties {:on-click :event}
                   :value      button
                   :examples   []}
          'text   {:properties {}
                   :value      text
                   :examples   []}})
