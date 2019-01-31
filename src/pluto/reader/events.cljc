(ns pluto.reader.events
  (:require [clojure.walk               :as walk]
            [pluto.reader.destructuring :as destructuring]
            [pluto.reader.errors        :as errors]
            [pluto.reader.reference     :as reference]
            [pluto.reader.types         :as types]
            [pluto.utils                :as utils]))

;; TODO part of this is duplicated from blocks/let

(defn replace-atom [env o]
  (cond (contains? env o) (get env o)
        (symbol? o) nil
        (string? o) (:data (utils/interpolate env o))
        (fn? o) #(o % env)
        :else (walk/postwalk-replace env o)))

(defn- resolve-env
  "Resolve pairs from `env` in `m`.
   Uses #replace-atom to perform the resolution."
  [env m]
  (reduce-kv #(assoc %1 %2 (replace-atom env %3)) {} m))

(defn- resolve-arguments
  "Resolve an event arguments based on event definition"
  [ctx ext event arguments]
  (if-let [type (get-in ctx [:capacities :events event :arguments])]
    (types/resolve ctx ext type arguments)
    {:errors [(errors/error ::errors/missing-reference-arguments {:type :event :value event})]}))

(defn- dispatch-events
  "Dispatches an event using ctx"
  [ctx events]
  (when-let [f (get-in ctx [:env :event-fn])]
    (if (seq events)
      (f events)
      (println "Empty event dispatched"))))

(defn- create-event [ctx env {:keys [data inline]}]
  [data (:env ctx) (resolve-env env inline)])

(defn- create-ref [ctx ext [event args :as reference]]
  (let [{data :data errors1 :errors} (reference/resolve ctx ext :event reference)
        {inline :data errors :errors} (resolve-arguments ctx ext event (or args {}))]
    {:data   data
     :inline inline
     :errors (concat errors errors1)}))

(defn- resolve-query
  "Resolve a query using ctx"
  [ctx ext query]
  (let [{data :data} (types/resolve ctx ext :query query)]
    (when-let [f (get-in ctx [:env :query-fn])]
      (when-let [signal (f data)]
        @signal))))

(defn merge-resolved-query [ctx ext m {:keys [value bindings]}]
  (cond
    (map? bindings)
    (merge m (:data (destructuring/destructure bindings (merge m (resolve-query ctx ext value)))))
    (symbol? bindings)
    (assoc m bindings (resolve-query ctx ext value))))

(defn- event-dispatcher
  "Returns a function of 2 arguments "
  [ctx ext refs arguments {:keys [queries properties]}]
  (let [ref (map #(create-ref ctx ext %) refs)]
    (errors/merge-errors
      {:data
       (with-meta
         (fn [dynamic env]
           ;; TODO env contains data that shouldn't be there
           ;; env is the dispatched argument. Used as default but is overridden by the local arguments
           ;; Perform destructuring based on dynamic and static arguments
           ;; Then resolve recursive properties in the aggregated env
           ;; Final map contains inline arguments resolved
           (let [{:keys [data errors]} (destructuring/destructure properties (merge dynamic arguments))]
             ;; TODO handle errors
             (let [env' (resolve-env env (merge env (reduce #(merge-resolved-query ctx ext %1 %2) data queries)))]
               (dispatch-events ctx (map #(create-event ctx env' %) ref)))))
         {:event true})}
      nil)))

(defn- references
  "Returns a list of local event references"
  [data]
  (drop 2 data))

(defn local-event?
  "A local event must define a let block and have a single destructuring binding accessing 'properties."
  [data]
  (when (list? data)
    (let [[form bindings] data]
      (and (< 2 (count data))
           (= 'let form)
           (even? (count bindings))
           (map? (first bindings))
           (= 'properties (second bindings))
           (every? reference/reference? (references data))))))

(defn- merge-pair [m [k v]]
  (cond
    (= v 'properties) (assoc m :properties k)
    :else (update m :queries concat [{:value v :bindings k}])))

(defn- parse-let-bindings [bindings]
  (let [pairs (partition 2 bindings)]
    (reduce merge-pair
            {}
            pairs)))

(defn parse
  "Parses local references defining let blocks"
  [ctx ext [_ let-bindings :as local] arguments]
  (if (local-event? local)
    (event-dispatcher ctx ext (references local) arguments (parse-let-bindings let-bindings))
    {:errors [(errors/error ::errors/invalid-local-event local)]}))

(defmethod types/resolve :event [ctx ext _ [_ arguments :as value]]
  (let [{:keys [data errors] :as m} (reference/resolve ctx ext :event value)]
    ;; resolve returns either data or errors
    (if data
      (merge (when data
               (if (keyword? data)
                 (event-dispatcher ctx ext (list value) arguments nil)
                 (parse ctx ext data arguments)))
             (when errors
               {:errors (apply conj [(errors/error ::errors/unknown-event symbol)] errors)}))
      m)))