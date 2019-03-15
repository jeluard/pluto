(ns ^:figwheel-hooks pluto.playground.figwheel
  (:require [re-frame.core :as re-frame]
            [devtools.core :as devtools]
            pluto.playground.core))

(enable-console-print!)
(devtools/install!)

(defn ^:before-load before-reload []
  (re-frame/clear-subscription-cache!)
  (println "Reloading ..."))

(defn ^:after-load after-reload []
  (println "Reloading done."))
