(ns pluto.playground.analyzer
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

(defn primitives [source]
  {:events {}
   :views {}})

(defn events [source])
  