(ns pluto.playground.markdown
  (:require [clojure.edn :as edn]
            [cljsjs.marked]))

(defn markdown->edn [s]
  (when s
    (let [v (js->clj (.lexer js/marked s))]
      (->> (filter #(= "code" (get % "type")) v)
           (into {} (map (fn [m] [(symbol (get m "lang")) (edn/read-string {} (get m "text"))])))))))