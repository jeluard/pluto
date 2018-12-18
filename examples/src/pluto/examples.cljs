(ns pluto.examples
  (:require [clojure.string        :as string]
            [pluto.components.html :as html]
            [pluto.js              :as js]
            [pluto.reader          :as reader]
            [pluto.reader.hooks    :as hooks]
            [pluto.storages        :as storages]
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

(defonce do-timer (js/setInterval #(re-frame/dispatch [:random (zero? (rand-int 2))]) 1000))

(re-frame/reg-event-db
  :random
  (fn [db [_ b]]
    (assoc db :random {:cond? b})))

(re-frame/reg-fx
 ::alert
 (fn [value] (js/alert value)))

(re-frame/reg-event-fx
  :alert
  (fn [cofx [_ env {:keys [value]}]]
    {::alert (str "id = " (:id env) " value = " value)}))

(re-frame/reg-fx
  ::identity
  (fn [{:keys [cb]}] (re-frame/dispatch (cb {:value "Yeahhh"}))))

(re-frame/reg-event-fx
  :identity
  (fn [cofx [_ env m]]
    {::identity m}))

(re-frame/reg-sub
  :random-boolean
  :random)

(re-frame/reg-sub :extensions/identity
                  (fn [_ [_ _ {:keys [value]}]] value))

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
                 :capacities {:components html/components
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

(defn ^:export load-and-render
  [s el el-errors]
  (dom/unmount-component-at-node el)
  (dom/unmount-component-at-node el-errors)
  (storages/fetch s #(render-result % el el-errors)))
