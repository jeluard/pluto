(ns pluto.playground.components.extension)

(defn extension [{:keys [data payload]}]
  ; use data to display  based on hook type, allow to select which hook to display
  (when-let [view (get-in data [:hooks :main.demo :view])]
    [view payload]))