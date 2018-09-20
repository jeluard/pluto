(ns pluto.reader.hooks
  (:require [clojure.string         :as string]
            [clojure.set            :as set]
            [pluto.host             :as host]
            [pluto.reader.blocks    :as blocks]
            [pluto.reader.errors    :as errors]
            [pluto.reader.reference :as reference]
            [pluto.reader.views     :as views]))

(defmulti resolve-property
          (fn [{:keys [type]} _ _ _]
            (cond
              (keyword? type) type
              (:one-of type) :set
              (set? type) :subset
              (map? type) :map
              (vector? type) :vector)))

(defn inject-properties [m properties]
  (if-let [ps (get-in m [:env 'properties])]
    (let [{:keys [data errors]} (blocks/destructure ps properties)]
      (errors/merge-errors
        {:data
         (-> (update m :env dissoc 'properties)
             (update :env merge data))}
        errors))
    {:data m}))

(defn hiccup-with-properties [h properties]
  (if (vector? h) 
    (let [[tag & properties-children] h
          [props children]            (views/resolve-properties-children properties-children)
          {:keys [data]}              (when properties
                                        (inject-properties props properties))]
      (apply conj (if data [tag data] [tag])
             (map #(hiccup-with-properties % properties) children)))
    h))

(defmethod resolve-property :view [def hook opts m]
  (let [{:keys [data] :as m}  (reference/resolve m def hook)
        {:keys [data errors]} (views/parse opts data)]
    ;; TODO Might fail at runtime if destructuring is incorrect
    (errors/merge-errors (when data {:data (fn [o] (hiccup-with-properties data o))})
                         (concat errors (:errors m)))))

(defn- resolve-capacities-value [key error-key capacities {:keys [name optional?]} hook]
  (if-let [value (get hook name)]
    (if-let [component ((get capacities key) value)]
      {:data component}
      {:errors [(errors/error error-key value)]})
    (when-not optional?
      {:errors [(errors/error ::errors/invalid-property-name name)]})))

(defmethod resolve-property :component [def hook {:keys [capacities]} _]
  (resolve-capacities-value :components ::errors/unknown-component capacities def hook))

(defmethod resolve-property :event [def hook {:keys [capacities]} m]
  (resolve-capacities-value :events ::errors/event-not-exposed capacities def hook))

(defmethod resolve-property :query [def hook {:keys [capacities]} _]
  (resolve-capacities-value :queries ::errors/query-not-exposed capacities def hook))

(defmethod resolve-property :function [def hook {:keys [capacities]} _]
  (resolve-capacities-value :functions ::errors/function-not-exposed capacities def hook)) 

(defn- resolve-property-value [f {:keys [name optional?]} hook]
  (if-let [o (get hook name)]
    (if (f o)
      {:data o}
      {:errors [(errors/error ::errors/invalid-property-value o)]})
    (when-not optional?
      {:errors [(errors/error ::errors/invalid-property-name name)]})))

(defmethod resolve-property :string [def hook _ _]
  (resolve-property-value string? def hook))

(defmethod resolve-property :keyword [def hook _ _]
  (resolve-property-value keyword? def hook))

(defmethod resolve-property :set [def hook _ _]
  (resolve-property-value (-> def :type :one-of) def hook))

(defmethod resolve-property :subset [def hook _ _] 
  (resolve-property-value #(set/subset? % (:type def)) def hook))

(declare parse-properties)

(defn map->properties [m]
  (reduce-kv #(conj %1 {:name %2 :type %3}) [] m))

(defmethod resolve-property :map [def hook opts m]
  (parse-properties (map->properties (:type def)) ((:name def) hook) opts m))

(defmethod resolve-property :vector [def hook opts m]
  (let [props (map->properties (first (:type def)))]
    (apply errors/merge-results-with #(conj (vec %1) %2) (map #(parse-properties props % opts m) ((:name def) hook)))))

(defmethod resolve-property :default [{:keys [type]} _ _ _]
  {:errors [(errors/error ::errors/invalid-property-type type)]})

(defn hook? [s]
  (= "hooks" (namespace s)))

(defn hooks [ext]
  (filter hook? (keys ext)))

(defn local-id [s]
  (keyword (string/join "." (rest (string/split (name s) #"\.")))))

(defn root-id [s]
  (keyword (first (string/split (name s) #"\."))))

(defn properties [opts s]
  (reduce-kv #(conj %1 {:name %2 :type %3}) []
             (get-in opts [:capacities :hooks s :properties])))

(defn normalize-property [{:keys [name] :as prop}]
  (let [normalized-name (keyword (string/replace (clojure.core/name name) "?" ""))]
    (assoc prop
      :name      normalized-name
      :optional? (not= name normalized-name))))

(defn parse-properties [props v opts m]
  (reduce #(let [{:keys [data errors]} (resolve-property (normalize-property %2) v opts m)]
             (errors/merge-errors (if data (assoc-in %1 [:data (:name (normalize-property %2))] data) %1) errors))
          {} props))

(defn parse-hook [hook v opts m]
  (parse-properties (map->properties (host/properties hook)) v opts m))

(defn parse [opts m]
  (reduce-kv (fn [acc hook-key data]
               (let [hook-id               (local-id hook-key)
                     hook-root             (root-id hook-key)
                     hook                  (get-in opts [:capacities :hooks hook-root]) 
                     {:keys [data errors]} (parse-hook hook data opts m)] 
                     
                 (errors/merge-errors
                  (-> acc
                      (assoc-in [:data :hooks hook-root hook-id :parsed] data)
                      (assoc-in [:data :hooks hook-root hook-id :hook-ref] hook))
                  errors)))
             {}
             (select-keys m (hooks m))))

