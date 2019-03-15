(ns pluto.playground.subs
  (:require [re-frame.core :as re-frame]))

(defn- extension-parsed
  [db v]
  (:parsed db))

(re-frame/reg-sub
  :extension/parsed
  extension-parsed)

(defn- extension-data
  [db v]
  (:data db))

(re-frame/reg-sub
  :extension/data
  extension-data)

(defn- extension-source
  [db v]
  (:source db))

(re-frame/reg-sub
  :extension/source
  extension-source)

(defn- extension-traces
  [db v]
  (:traces db))

(re-frame/reg-sub
  :extension/traces
  extension-traces)

(defn- selected
  [db v]
  (:selected db))

(re-frame/reg-sub
  :extension/selected
  selected)

(defn- preview
  [db v]
  (or (:preview db) false))

(re-frame/reg-sub
  :extension/preview
  preview)