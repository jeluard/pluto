(ns pluto.reader.types
  "Resolve values based on provided types.
   Handles primitives, references and composed values."
  (:refer-clojure :exclude [resolve])
  (:require [clojure.string             :as string]
            [clojure.set                :as set]
            [clojure.walk               :as walk]
            [pluto.reader.destructuring :as destructuring]
            [pluto.reader.errors        :as errors]
            [pluto.reader.reference     :as reference]
            [pluto.utils                :as utils]))

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

(defn- resolve-arguments [ctx ext event arguments]
  (if-let [type (get-in ctx [:capacities :events event :arguments])]
    (resolve ctx ext type arguments)
    {:errors [(errors/error ::errors/missing-reference-arguments {:type :events :value event})]}))

;; TODO part of this is duplicated from blocks/let

(defn replace-atom [env o]
  (cond (contains? env o) (get env o)
        (symbol? o) nil
        (string? o) (:data (utils/interpolate env o))
        (fn? o) #(o % env)
        :else (walk/postwalk-replace env o)))

(defn symbol-map->keyword-map [m]
  (reduce-kv #(assoc %1 (keyword (name %2)) %3) {} m))

(defn keyword-map->symbol-map [m]
  (reduce-kv #(assoc %1 (symbol (name %2)) %3) {} m))

(defn event-after-env [ctx ref inline static bindings]
  (with-meta
    (fn [dynamic env]
      ;; env is the dispatched argument. Used has default but is overriden by the local arguments
      ;; Perform destructuring based on dynamic and static arguments
      ;; Then resolve recursive properties in the aggregated env
      ;; Final map is contains inline arguments resolved
      (let [env' (reduce-kv #(assoc %1 %2 (if (contains? env %3) (get env %3) %3)) {}
                            (merge env (:data (destructuring/destructure bindings (merge dynamic static)))))]
        [ref (:env ctx) (reduce-kv #(assoc %1 %2 (replace-atom env' %3)) {} inline)]))
    {:event true}))

(defn- event-reference-with-arguments [ctx ext ref event arguments args bindings]
  (if arguments
    (let [{:keys [data errors]} (resolve-arguments ctx ext event arguments)]
      (errors/merge-errors {:data (event-after-env ctx ref data args bindings)} errors))
    {:data (fn [o v] [ref (:env ctx) o])}))

(defn- reference-symbol [value]
  (nth value 2))

(defn local-event?
  "A local event must define a let block and have a single destructuring binding accessing 'properties."
  [data]
  (when (list? data)
    (let [[form bindings] data]
      (and (= 3 (count data))
           (= 'let form)
           (= 2 (count bindings))
           (map? (first bindings))
           (= 'properties (second bindings))
           (let [s (reference-symbol data)]
             (or (vector? s)
                 (and (list? s) (= 'if (first s)))))))))

(defn resolve-local-event [ctx ext type data]
  (let [a (reference-symbol data)]
    (cond
      (vector? a)
      (let [[event args :as reference] a
            bindings (first (second data))
            {:keys [data errors]} (reference/resolve ctx ext type reference)]
        (errors/merge-errors {:data {:ref data :event event :args args :bindings bindings}} errors)))))

(defn resolve-local-reference
  "References local references defining let blocks"
  [ctx ext type [name :as value]]
  (let [{:keys [data errors] :as m} (reference/resolve ctx ext type value)]
    ;; resolve returns either data or errors
    (if data
      (cond
        (local-event? data)
        (resolve-local-event ctx ext type data)
        (keyword? data)
        {:data {:ref data :event name}}
        :else
        {:errors [(errors/error ::errors/invalid-local-event data)]})
      m)))

(defn resolve-event [ctx ext type [symbol arguments :as value]]
  (let [{:keys [data errors]}   (resolve-local-reference ctx ext type value)
        {event :event ref :ref args :args bindings :bindings} data]
    ;; TODO better separate local event handling
    (merge (when data (event-reference-with-arguments ctx ext ref event (or args arguments) arguments bindings))
           (when errors
             {:errors (apply conj [(errors/error ::errors/unknown-event symbol)] errors)}))))

(defmethod resolve :event [ctx ext type [name arguments :as value]]
  (resolve-event ctx ext type value))

(defmethod resolve :query [{:keys [env] :as ctx} ext type [name arguments :as value]]
  (let [{:keys [data errors]} (reference/resolve ctx ext type value)]
    (merge (when data {:data (if arguments [data env arguments] [data env])})
           (when errors
             {:errors (apply conj [(errors/error ::errors/unknown-query name)] errors)}))))

(defmethod resolve :default [_ _ type value]
  {:errors [(errors/error ::errors/invalid-type (merge {:type type} (when value {:value value})))]})
