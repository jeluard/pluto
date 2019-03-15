(ns pluto.playground.components.capacities
  (:require [reagent.core :as reagent]))

(defn section [{:keys [title on-click]} & children]
  [:div {:class "section" :on-click on-click}
   [:h4 title]
   (into [:div] children)])

(defn subsection [{:keys [title]} & children]
  [:div {:class "subsection"}
   [:h5 title]
   (into [:div] children)])

(defn property [[k v]]
  [:div
   (str (name k) "." v)])

(defn permission [s]
  [:div
   (str s)])

(defn component [[s {:keys [value permissions properties description examples]}]]
  [section {:title (str s) :on-click #(.log js/console (reagent/as-element (value)))}
   [:div description]
   #_[subsection {:title "Properties"}
      (map (fn [[k :as o]] ^{:key k} [property o]) properties)]
   #_[subsection {:title "Permissions"}
      (map (fn [o] ^{:key o} [permission o]) permissions)]])

(defn query [[s {:keys [permissions arguments]}]]
  [section {:title (str s)}
   [:div (str arguments)]])

(defn event [[s {:keys [permissions arguments]}]]
  [section {:title (str s)}
   [:div (str arguments)]])

(defn tree [{:keys [components queries events]}]
  [:div
   [:div "Components"
    (for [[s :as m] components]
      ^{:key s}
      [component m])]
   [:div "Queries"
    (for [[s :as m] queries]
      ^{:key s}
      [query m])]
   #_[:div "Events"
      (for [[s :as m] events]
        ^{:key s}
        [event m])]])