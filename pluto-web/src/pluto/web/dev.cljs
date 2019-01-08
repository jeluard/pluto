(ns pluto.web.dev
  (:require [reagent.dom      :as dom]
            [re-frame.loggers :as loggers]
            [pluto.storages   :as storages]))

(def warn (js/console.warn.bind js/console))
(loggers/set-loggers!
  {:warn (fn [& args]
           (cond
             (= "re-frame: overwriting" (first args)) nil
             :else (apply warn args)))})
