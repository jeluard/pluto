(ns pluto.web.events
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-fx
 ::alert
 (fn [value] (js/alert value)))

(re-frame/reg-event-fx
  :alert
  (fn [_ [_ env {:keys [value]}]]
    {::alert (str "id = " (:id env) " value = " value)}))

(re-frame/reg-fx
  ::identity
  (fn [{:keys [cb]}] (cb {})))

(re-frame/reg-event-fx
  :identity
  (fn [_ [_ _ m]]
    {::identity m}))
