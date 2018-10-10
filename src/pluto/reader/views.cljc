(ns pluto.reader.views
  (:require [clojure.spec.alpha         :as spec]
            [pluto.reader.blocks        :as blocks]
            [pluto.reader.destructuring :as destructuring]
            [pluto.reader.errors        :as errors]
            [pluto.reader.permissions   :as permissions]
            [pluto.reader.reference     :as reference]
            [pluto.reader.types         :as types]
            [pluto.utils                :as utils]))

;; TODO Distinguish views (can contain blocks, symbols) validation
;; from hiccup validation (view after parsing) that are pure hiccup

(spec/def ::form
  (spec/or
   :string  string?
   :number  number?
   :symbol  symbol?
   :element vector?
   :block   list?))

(spec/def ::property-map (spec/map-of keyword? any?))

(spec/def ::element
  (spec/cat
   :tag      (spec/or :symbol symbol? :fn fn?)
   :attrs    (spec/? map?) 
   :children (spec/* ::form)))

(declare parse)

(defn parse-hiccup-children [ctx ext children]
  (reduce #(let [{:keys [data errors]} (parse ctx ext %2)]
             (errors/merge-errors (update %1 :data conj data)
                                  errors))
          {:data []} children))

(defn component? [o]
  (symbol? o))

(defn- resolve-component [ctx o]
  (cond
    (fn? o) o
    (symbol? o) (get-in ctx [:capacities :components o :value])))

(defmulti resolve-default-component-properties (fn [property value] property))

(defmethod resolve-default-component-properties :style [_ value]
  {:data value})

(defmethod resolve-default-component-properties :default [_ value]
  nil)

(defn- resolve-component-property [ctx ext component k v]
  (or (resolve-default-component-properties k v)
      (if-let [type (k (get-in ctx [:capacities :components component :properties]))]
        (types/resolve ctx ext type v)
        {:errors [(errors/error ::errors/unknown-component-property {:component component :property k})]})))

(defn- resolve-property [ctx ext component k v]
  (if (component? component)
    (resolve-component-property ctx ext component k v)
    {:data v}))

(defn- resolve-component-properties [ctx ext component properties]
  (if-let [explain (spec/explain-data ::property-map properties)]
    {:errors [(errors/error ::errors/invalid-property-map properties {:explain-data explain})]}
    (reduce-kv (fn [acc k v]
                 (let [{:keys [data errors]} (resolve-property ctx ext component k v)]
                   (errors/merge-errors (update acc :data assoc k data)
                                        errors)))
               {:data   {}
                :errors []}
               properties)))

(defn- resolve-properties-children [[properties? & children]]
  [(and (map? properties?) properties?)
   (cond
     (map? properties?) children
     (not (nil? properties?)) (cons properties? children)
     :else children)])

(defn- parse-hiccup-element [ctx ext o]
  (let [explain
        (if (vector? o) ;; this eliminates spec explain data noise
          (spec/explain-data ::element o)
          (spec/explain-data ::form o))]
    (cond
      ;; TODO Validate views, not hiccup
      (not (nil? explain))
      {:errors [(errors/error ::errors/invalid-view o {:explain-data explain})]}

      (or (symbol? o) (utils/primitive? o)) {:data o}

      (vector? o)
      (let [[element & properties-children]  o
            [properties children]            (resolve-properties-children properties-children)
            component                        (resolve-component ctx element)
            {:keys [data errors]}            (when properties
                                               (resolve-component-properties ctx ext element properties))]
        (cond-> (let [m (parse-hiccup-children ctx ext children)]
                  ;; Reduce parsed children to a single map and wrap them in a hiccup element
                  ;; whose component has been translated to the local platform
                  (update m :data #(apply conj (if data [(or component element) data]
                                                   [(or component element)])
                                          %)))
                (nil? component) (errors/accumulate-errors [(errors/error ::errors/unknown-component element)])
                (seq errors)     (errors/accumulate-errors errors))))))

(defn parse [ctx ext o]
  (cond
    (list? o)
    (let [{:keys [data errors]} (blocks/parse ctx ext o)]
      (if errors
        {:errors errors}
        (parse ctx ext data)))
    :else
    (parse-hiccup-element ctx ext o)))

(defn- inject-properties [m properties]
  (if-let [ps (get-in m [:env 'properties])]
    (let [{:keys [data errors]} (destructuring/destructure ps properties)]
      (errors/merge-errors
        {:data
         (-> m
             (update :env dissoc 'properties)
             (update :env merge data))}
        errors))
    {:data m}))

(defn- hiccup-with-properties [h properties]
  (if (vector? h)
    (let [[tag & properties-children] h
          [props children]            (resolve-properties-children properties-children)
          {:keys [data]}              (when properties
                                        (inject-properties props properties))]
      (apply conj (if data [tag data] [tag])
             (map #(hiccup-with-properties % properties) children)))
    h))

(defmethod types/resolve :view [ctx ext type value]
  (let [{:keys [data errors]} (reference/resolve ctx ext type value)]
    (if data
      (let [{:keys [data errors]} (parse ctx ext data)]
        ;; TODO Might fail at runtime if destructuring is incorrect
        (errors/merge-errors (when data {:data (fn [o] (hiccup-with-properties data o))})
                             (concat errors (:errors ext))))
      {:errors errors})))
