(ns pluto.playground.fx
  (:require [re-frame.core :as re-frame]
            [pluto.core    :as pluto]))

(defn- update-extension-parsed
  [{:keys [db]} [_ data]]
  {:db (assoc db :parsed data)})

(re-frame/reg-event-fx
  :extension/update-parsed
  update-extension-parsed)

(re-frame/reg-fx
  :extension/parse
  (fn [[ctx data]]
    (let [{:keys [data errors]} (pluto/parse ctx data)]
      (re-frame/dispatch [:extension/update-parsed data]))))

(defn- update-extension-data
  [{:keys [db]} [_ ctx data]]
  {:db (assoc db :data data)
   :extension/parse [ctx data]})

(re-frame.core/reg-event-fx
  :extension/update-data
  update-extension-data)

(re-frame.core/reg-fx
  :extension/read
  (fn [[ctx source]]
    (let [{:keys [data errors]} (pluto/read source)]
      (re-frame/dispatch [:extension/update-data ctx data]))))

(defn- update-extension-source
  [{:keys [db]} [_ ctx data]]
  {:db             (assoc db :source data)
   :extension/read [ctx data]})

(re-frame.core/reg-event-fx
  :extension/update-source
  update-extension-source)

(defn- append-trace
  [{:keys [db]} [_ data]]
  {:db (update db :traces conj data)})

(re-frame.core/reg-event-fx
  :extension/append-trace
  append-trace)

(defn- set-selected
  [{:keys [db]} [_ path]]
  {:db (assoc db :selected path)})

(re-frame.core/reg-event-fx
  :extension/set-selected
  set-selected)

(defn- switch-preview
  [{:keys [db]} [_]]
  {:db (update db :preview not)})

(re-frame.core/reg-event-fx
  :extension/switch-preview
  switch-preview)