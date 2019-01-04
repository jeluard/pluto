(ns pluto.web.components
  (:require [re-frame.core :as re-frame]))

(defn view [props & content]
  (into [:div props] content))

(defn button [{:keys [on-click] :as m} & content]
  (into [:button {:on-click #(on-click {})}] content))

(defn text [props & content]
  (into [:span props] content))

(def all {'view   {:properties {}
                   :data       view
                   :description ""
                   :examples   []}
          'button {:properties {:on-click :event}
                   :data       button
                   :examples   []}
          'text   {:properties {}
                   :data       text
                   :examples   []}})
