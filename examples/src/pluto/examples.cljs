(ns pluto.examples
  (:require [pluto.web.components  :as components]
            pluto.web.events
            pluto.web.queries
            [pluto.core            :as pluto]
            [pluto.storages        :as storages]
            [pluto.web.editor.markdown :as mk]
            pluto.reader.views
            [reagent.core          :as reagent]
            [reagent.dom           :as dom]
            [re-frame.core         :as re-frame]
            [re-frame.loggers      :as re-frame.loggers]))

(def warn (js/console.warn.bind js/console))
(re-frame.loggers/set-loggers!
  {:warn (fn [& args]
           (cond
             (= "re-frame: overwriting" (first args)) nil
             :else (apply warn args)))})

(defn render [h el]
  (reagent/render (h {:name "Test Extension"
                      :users [{:nm "Jane"}
                              {:nm "Sue"}]}) el))

(defn errors-list [v]
  (fn []
    [:div
     [:div "Errors"]
     (into [:ul]
       (for [[_ {type :type :as m}] v]
         [:li
          [:span [:b (str type)] (pr-str (dissoc m :type))]]))]))

(defn dispatch-events [events]
  (doseq [event events]
    (re-frame/dispatch event)))

(defn parse [m]
  (pluto/parse {:env        {:id       "Extension ID"
                             :logger   nil
                             :event-fn dispatch-events
                             :query-fn nil}
                :capacities {:components components/all
                             :queries    {'random-boolean
                                          {:data :random-boolean}
                                          'identity
                                          {:data      :extensions/identity
                                           :arguments {:value :map}}}
                             :hooks      {:main
                                          {:properties {:view :view}}}
                             :events     {'identity
                                          {:permissions [:read]
                                           :data        :identity
                                           :arguments   {:cb :event}}
                                          'alert
                                          {:permissions [:read]
                                           :data        :alert
                                           :arguments   {:value :string}}}}}
               m))

(defn render-extension [m el el-errors]
  (let [{:keys [data errors]} (parse m)]
    (when errors
      (render (errors-list errors) el-errors))
    (if-let [view (get-in data [:hooks :main.demo :view])]
      (render view el)
      (render (fn [] [:div "Oups"]) el))))

(defn read-extension [o el el-errors]
  (let [{:keys [data errors]} (pluto/read (:content o))]
    (render-extension data el el-errors)))

(defn render-result [{:keys [type value]} el el-errors]
  (case type
    :error (set! (.-innerHTML el-errors) value)
    (read-extension value el el-errors)))

(defn ^:export load-and-render
  [s el el-errors]
  (dom/unmount-component-at-node el)
  (dom/unmount-component-at-node el-errors)
  (storages/fetch s #(render-result % el el-errors)))
