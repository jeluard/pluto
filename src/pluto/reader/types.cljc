(ns pluto.reader.types
  "Resolve values based on provided types.
   Handles primitives, references and composed values."
  (:refer-clojure :exclude [resolve])
  (:require [clojure.set            :as set]
            [clojure.string         :as string]
            [pluto.reader.errors    :as errors]
            [pluto.reader.reference :as reference]))

(def reference-types #{:view :event :query})

(defmulti resolve
  "Resolve a value based on a type.
   Returns a map of either:
    * data with the resolved data
    * errors encapsulating all errors generated during resolution"
  (fn [ctx ext type value]
    (cond
      (symbol? value) :symbol
      (:one-of type)  :one-of
      (:or type)      :or
      (keyword? type) type
      (set? type)     :subset
      (map? type)     :assoc
      (vector? type)  :sequence)))

(defmethod resolve :symbol [_ _ _ value]
  ;; TODO properly validate symbols based on inferred type
  {:data value})

(defn invalid-type-value [type value]
  (errors/error ::errors/invalid-type-value {:type type :value value}))

(defmethod resolve :any [_ _ _ value]
  {:data value})

(defmethod resolve :boolean [_ _ _ value]
  (if (boolean? value)
    {:data value}
    {:errors [(invalid-type-value :boolean value)]}))

(defmethod resolve :number [_ _ _ value]
  (if (number? value)
    {:data value}
    {:errors [(invalid-type-value :number value)]}))

(defmethod resolve :string [_ _ _ value]
  (if (string? value)
    {:data value}
    {:errors [(invalid-type-value :string value)]}))

(defmethod resolve :keyword [_ _ _ value]
  (if (keyword? value)
    {:data value}
    {:errors [(invalid-type-value :keyword value)]}))

(defmethod resolve :vector [_ _ _ value]
  (if (vector? value)
    {:data value}
    {:errors [(invalid-type-value :vector value)]}))

(defmethod resolve :map [_ _ _ value]
  (if (map? value)
    {:data value}
    {:errors [(invalid-type-value :map value)]}))

(defmethod resolve :subset [_ _ type value]
  (if (and (set? value) (set/subset? value type))
    {:data value}
    {:errors [(invalid-type-value :subset value)]}))

(defmethod resolve :sequence [ctx ext type value]
  (if (and (vector? type) (= 1 (count type)) (map? (first type)))
    (apply errors/merge-results-with #(conj (vec %1) %2) (map #(resolve ctx ext (first type) %) value))
    {:errors [(errors/error ::errors/invalid-sequential-type {:type type :value value})]}))

(defmethod resolve :one-of [_ _ {:keys [one-of]} value]
  (if-let [o (one-of value)]
    {:data o}
    {:errors [(invalid-type-value :one-of value)]}))

(defmethod resolve :or [ctx ext {:keys [or]} value]
  (if (coll? or)
    (if-let [o (some #(when-let [{:keys [data]} (resolve ctx ext % value)] data) or)]
      {:data o}
      {:errors [(invalid-type-value :or value)]})
    {:errors [(invalid-type-value :or value)]}))

(def ^:private sentinel ::sentinel)

(defn- property [name value]
  (let [normalized-name (keyword (string/replace (clojure.core/name name) "?" ""))]
    {:value     (get value normalized-name sentinel)
     :name      normalized-name
     :optional? (not= name normalized-name)}))

(defn- resolve-property [ctx ext m {:keys [name optional? value]} type]
  (if (not= sentinel value)
    (let [{:keys [data errors]} (resolve ctx ext type value)]
      (errors/merge-errors
        (if data (assoc-in m [:data name] data) m)
        errors))
    (if optional?
      (update m :data #(if (empty? %) {} %))
      (assoc m :errors [(errors/error ::errors/missing-property name)]))))

(defmethod resolve :assoc [ctx ext type value]
  (if (map? type)
    (reduce-kv #(resolve-property ctx ext %1 (property %2 value) %3)
               {} type)
    {:errors [(errors/error ::errors/invalid-assoc-type {:type type :value value})]}))

;; TODO replace with generic reference resolution?
;; reference resolution: first lookup ctx, then local primitives if supported

(defmethod resolve :query [{:keys [env] :as ctx} ext type [name arguments :as value]]
  (let [{:keys [data errors]} (reference/resolve ctx ext type value)]
    (merge (when data {:data (if arguments [data env arguments] [data env])})
           (when errors
             {:errors (apply conj [(errors/error ::errors/unknown-query name)] errors)}))))

(defmethod resolve :default [_ _ type value]
  {:errors [(errors/error ::errors/invalid-type (merge {:type type} (when value {:value value})))]})
