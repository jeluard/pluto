(ns pluto.reader.errors
  "Inspired by https://github.com/cognitect-labs/anomalies"
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::type #{::reader-error
                   ::invalid-meta
                   ::invalid-key
                   ::invalid-value
                   ;; Types
                   ::invalid-type
                   ::invalid-type-name
                   ::invalid-type-value
                   ::invalid-sequential-type
                   ::invalid-assoc-type
                   ::invalid-reference
                   ::invalid-destructuring-format
                   ::missing-property
                   ::missing-keys
                   ::unknown-reference
                   ::unknown-reference-type
                   ::unknown-component
                   ::unknown-component-property
                   ::unknown-query
                   ::unknown-event
                   ::invalid-view
                   ::invalid-property-map                   
                   ::invalid-block
                   ::unsupported-test-type})

(spec/def ::value any?)

(spec/def ::message string?)

(spec/def ::error (spec/keys :req [::type ::value]
                             :opt [::message]))

(defn error
  ([type o] (error type o {}))
  ([type o m]
   {:pre [(spec/valid? ::type type)]}
   (assoc m ::type type ::value o)))

(defn accumulate-errors [m s]
  (update m :errors concat s))

(defn merge-errors [m errors]
  (cond-> m
          (seq errors) (accumulate-errors errors)))

(defn update-errors [m errors]
  (if (seq errors)
    (update m :errors concat errors)
    m))

(defn update-data [m f data]
  (if data
    (update m :data f data)
    m))

(defn merge-result
  ([m mm] (merge-result merge m mm))
  ([f m {:keys [data errors]}]
   (-> m
       (update-data f data)
       (update-errors errors))))

(defn merge-results [& ms]
  (reduce merge-result {} ms))

(defn merge-results-with [f & ms]
  (reduce #(merge-result f %1 %2) {} ms))
