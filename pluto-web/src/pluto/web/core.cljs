(ns pluto.web.core
  (:require [reagent.core          :as reagent]
            [pluto.core            :as pluto]
            pluto.reader.views
            [pluto.web.components  :as components]
            pluto.web.events
            pluto.web.queries))

(defn render [h el]
  (reagent/render (h {:name "Test Extension"
                      :users [{:nm "Jane"}
                              {:nm "Sue"}]}) el))

(defn errors-list [v]
  (fn []
    [:div
     [:div "Errors"]
     (into [:ul]
       (for [{:keys [type] :as m} v]
         [:li
          [:span [:b (str type)] (pr-str (dissoc m :type))]]))]))

(defn parse [m]
  (pluto/parse {:env        {:id "Extension ID"}
                :capacities {:components
                             {'view   {:properties {}
                                       :data       components/view
                                       :description ""
                                       :examples   []}
                              'button {:properties {:on-click :event}
                                       :data       components/button
                                       :examples   []}
                              'text   {:properties {}
                                       :data       components/text
                                       :examples   []}}
                             :queries
                             {'random-boolean
                              {:data :random-boolean}
                              'identity
                              {:data :extensions/identity :arguments {:value :map}}}
                             :hooks
                             {:main
                              {:properties {:view :view}}}
                             :events
                             {'identity
                              {:permissions [:read]
                               :data       :identity
                               :arguments   {:cb :event}}
                              'alert
                              {:permissions [:read]
                               :data       :alert
                               :arguments   {:value :string}}}}}
               m))

(defn render-extension [m el el-errors]
  (let [{:keys [data errors]} (parse m)]
    (when errors
      (render (errors-list errors) el-errors))
    (render (get-in data [:hooks :main :demo :parsed :view]) el)))

(defn read-extension [o el el-errors]
  (let [{:keys [data errors]} (pluto/read (:content o))]
    (render-extension data el el-errors)))

(defn render-result [{:keys [type value]} el el-errors]
  (case type
    :error (set! (.-innerHTML el-errors) value)
    (read-extension value el el-errors)))
