(ns pluto.demo
  (:require [pluto.components.html :as html]
            [pluto.reader :as reader]
            [pluto.storage :as storage]
            [pluto.storage.http :as http]
            [pluto.storage.ipfs :as ipfs]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frame.loggers :as re-frame.loggers]
            [devtools.core :as devtools]))

(enable-console-print!)
(devtools/install!)

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
    (assoc db :random b)))

(re-frame/reg-sub
  :random-boolean
  :random)

(defn render [h]
  (let [frame (js/document.getElementById "frame")]
   (reagent/render (h) (.. (aget frame "contentWindow" "document") -body -firstChild))))

(defn header []
  [:div
   "Random boolean: "
   (let [b @(re-frame/subscribe [:random-boolean])]
     [:span {:style {:color (if b :green :red)}}
      (str b)])])

(defn wrap-with-cartouche [o]
  (println ">>>>" o)
  (fn []
    [:div
     ^{:key 1} [header]
     ^{:key 2} o]))

(defn wrap-extensions [os]
  (fn []
    [:div
     (for [o os]
       ^{:key o} [:div
                  [:h2 (:name o)]
                  [:p (:content o)]])]))

(defn ^:export run
  []
  (re-frame/clear-subscription-cache!)

  (storage/fetch (ipfs/IPFSStorage. "http://localhost:8080")
    {:id "QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG" :URL "/demo.edn"}
    #(->> %
         wrap-extensions
         render))

  (storage/fetch (http/HTTPStorage.)
    {:URL "/demo.edn"}
    #(-> %
         reader/read
         :data
         ((fn [m] (println "<<<" m) (reader/parse {:components html/components :valid-hooks #{:hooks/main}} m)))
         :data
         :views/main
         wrap-with-cartouche
         render)))
