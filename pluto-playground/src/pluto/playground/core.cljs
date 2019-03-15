(ns pluto.playground.core
  (:require [pluto.playground.components.capacities :as capacities]
            [pluto.playground.components.extension  :as extension]
            [pluto.playground.components.inspector  :as inspector]
            [pluto.playground.components.source     :as source]
            [pluto.playground.components.traces     :as traces]
            pluto.playground.fx
            pluto.playground.subs
            pluto.reader.events
            pluto.reader.views
            [pluto.storages                         :as storages]
            [pluto.trace                            :as trace]
            [pluto.web.components                   :as components]
            pluto.web.events
            pluto.web.queries
            [reagent.core                           :as reagent]
            [re-frame.core                          :as re-frame]
            [re-frame.registrar                     :as registrar]
            [re-frame.loggers                       :as re-frame.loggers]))

(def warn (js/console.warn.bind js/console))
(re-frame.loggers/set-loggers!
  {:warn (fn [& args]
           (cond
             (= "re-frame: overwriting" (first args)) nil
             :else (apply warn args)))})

(defn- dispatch-events [ctx events]
  (doseq [event events]
    (if (vector? event)
      (re-frame/dispatch event)
      (trace/trace ctx (trace/create-trace :error :event/dispatch event)))))

(defn- resolve-query [ctx [id :as data]]
  (if (registrar/get-handler :sub id)
    (re-frame/subscribe data)
    (trace/trace ctx (trace/create-trace :error :query/resolve data))))

(defn cartouche [{:keys [path]} data]
  (let [p @(re-frame/subscribe [:extension/selected])]
    [:div {:on-mouse-enter #(re-frame/dispatch [:extension/set-selected path])
           :on-mouse-leave #(re-frame/dispatch [:extension/set-selected nil])
           :style (when (= p path) {:border "1px solid black"})}
     data]))

(defn wrap-view [parent-ctx {:keys [data] :as m}]
  (if (vector? data)
    {:data
     [cartouche parent-ctx
      data]}
    m))

(def ctx
  {:env        {:id "Extension ID"}
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
                              :arguments   {:value :string}}}}
   :event-fn dispatch-events
   :query-fn resolve-query
   :view-fn  wrap-view
   :tracer   #(re-frame/dispatch [:extension/append-trace %])})

(def payload
  {:name "Test Extension"
   :users [{:nm "Jane"}
           {:nm "Sue"}]})

;; TODO list all views/events/queries used (per category)
;; TODO source viewer

(def Switch (aget js/MaterialUI "Switch"))
(def Button (aget js/MaterialUI "Button"))
(def Dialog (aget js/MaterialUI "Dialog"))
(def DialogTitle (aget js/MaterialUI "DialogTitle"))

(defn publish [s]
  ())

(defn layout [{:keys [capacities] :as ctx}]
  (let [s       (re-frame/subscribe [:extension/source])
        m       @(re-frame/subscribe [:extension/data])
        data    @(re-frame/subscribe [:extension/parsed])
        v       @(re-frame/subscribe [:extension/traces])
        preview @(re-frame/subscribe [:extension/preview])]
    [:<>
     #_
     [:aside {:id "left"}
      [capacities/tree capacities]]
     [:main
      [:div {:style {:display "flex" :align-items "center" :justify-content "flex-end"}}
       [:> Button {:color "primary" :variant "contained" :on-click #(publish s)}
        "Publish"]
       #_
       [:> Dialog {:open true}
        [:> DialogTitle
         "Ehhhh"]]
       [:> Switch {:value   preview
                   :on-change #(re-frame/dispatch [:extension/switch-preview])}]
       [:div "Preview"]]
      [:div {:id "content"}
       (if preview
         [source/viewer {:content   s
                         :on-change #(re-frame.core/dispatch [:extension/update-source ctx %])}]
         [:div {:id "extension"}
          [extension/extension {:data data :payload payload}]])]
      [traces/table v]]
     #_ 
     [:aside {:id "right"}
      [inspector/tree ctx
       m]]]))

(defn- on-extension-read [ctx {:keys [type value]}]
  (re-frame.core/dispatch [:extension/update-source ctx (:content value)]))

(defn ^:export bootstrap
  [s]
  (storages/fetch s #(on-extension-read ctx %))
  (reagent/render
    [layout ctx]
    (.-body js/document)))