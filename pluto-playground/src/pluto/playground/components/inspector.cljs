(ns pluto.playground.components.inspector
  (:require [pluto.reader.types :as types]))

(defn primitive? [[k]]
  (let [n (namespace k)]
    (and n (#{"views" "events"} n))))

(defn fire-event [{:keys [event-fn] :as ctx} ext event arguments]
  (let [{:keys [data]} (types/resolve ctx ext :event [event arguments])]
    (data ctx [[event arguments]])))

(defn trigger-query [query arguments]
  (.log js/console "trigger" query arguments))

(defn tree [ctx m]
  (println m)
  [:div
   (for [[k v] (filter primitive? m)] ;; TODO only events and views?
     ^{:key k}
     [:div
      [:div (str k)]
      [:button {:on-click #(fire-event ctx m k {})}
       "Fire"]])])