(ns pluto.web.core
  (:require [reagent.core          :as reagent]
            [pluto.reader          :as reader]
            [pluto.reader.hooks    :as hooks]
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

(def hook 
  (reify hooks/Hook
    (hook-in [_ id env {:keys [description scope parameters preview short-preview]} cofx])
    (unhook [_ id env {:keys [scope]} {:keys [db] :as cofx}])))

(defn parse [m]
  (reader/parse {:env        {:id "Extension ID"}
                 :capacities {:components
                              {'view   {:properties {}
                                        :value      components/view
                                        :description ""
                                        :examples   []}
                               'button {:properties {:on-click :event}
                                        :value      components/button
                                        :examples   []}
                               'text   {:properties {}
                                        :value      components/text
                                        :examples   []}}
                              :queries    {'random-boolean
                                           {:value :random-boolean}
                                           'identity
                                           {:value :extensions/identity :arguments {:value :map}}}
                              :hooks      {:main
                                           {:hook       hook
                                            :properties {:view :view}}}
                              :events     {'identity
                                           {:permissions [:read]
                                            :value       :identity
                                            :arguments   {:cb :event}}
                                           'alert
                                           {:permissions [:read]
                                            :value       :alert
                                            :arguments   {:value :string}}}}}
                m))

(defn render-extension [m el el-errors]
  (let [{:keys [data errors]} (parse m)]
    (when errors
      (render (errors-list errors) el-errors))
    (render (get-in data [:hooks :main :demo :parsed :view]) el)))

(defn read-extension [o el el-errors]
  (let [{:keys [data errors]} (reader/read (:content o))]
    (render-extension data el el-errors)))

(defn render-result [{:keys [type value]} el el-errors]
  (case type
    :error (set! (.-innerHTML el-errors) value)
    (read-extension value el el-errors)))
