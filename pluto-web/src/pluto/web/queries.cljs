(ns pluto.web.queries
  (:require [re-frame.core :as re-frame]))

(defonce do-timer (js/setInterval #(re-frame/dispatch [:random (zero? (rand-int 2))]) 1000))

(re-frame/reg-event-db
  :random
  (fn [db [_ b]]
    (assoc db :random {:cond? b})))

(re-frame/reg-sub
  :random-boolean
  :random)

(re-frame/reg-sub :extensions/identity
                  (fn [_ [_ _ {:keys [value]}]] value))
