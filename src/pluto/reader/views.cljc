(ns pluto.reader.views
  (:require [clojure.spec.alpha         :as spec]
            [pluto.reader.blocks        :as blocks]
            [pluto.reader.errors        :as errors]
            [pluto.reader.reference     :as reference]
            [pluto.reader.types         :as types]
            [pluto.utils                :as utils]))

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

(defn- resolve-component [ctx ext [element :as o]]
  (cond
    (fn? element) element ;; TODO better abstract blocks
    (symbol? element) (or (get-in ctx [:capacities :components element :value])
                          ; First resolve using default components then lookup for local views
                          (:data (types/resolve ctx ext :view o)))))

(defmulti resolve-default-component-properties
  "Resolve default properties available for all components."
  (fn [property value] property))

(defmethod resolve-default-component-properties :style [_ value]
  {:data value})

(defmethod resolve-default-component-properties :default [_ _]
  nil)

(defn resolve-custom-component-properties [ctx ext component k v]
  (if-let [type (get-in ctx [:capacities :components component :properties k])]
    (if-not (and (types/reference-types type) (not (#{:event :view} type)))
      ;; TODO Infer symbol types and fail if type does not match
      (if-not (symbol? v)
        (let [{:keys [data errors]} (types/resolve ctx ext type v)]
          (errors/merge-errors (when data {:data data}) errors))
        {:data v})
      {:errors [(errors/error ::errors/invalid-component-property-type {:component component :property k :type type})]})
    (if (types/resolve ctx ext :view v)
      {:data v}
      {:errors [(errors/error ::errors/unknown-component-property {:component component :property k})]})))

(defn- resolve-component-property [ctx ext component k v]
  (or (resolve-default-component-properties k v)
      (resolve-custom-component-properties ctx ext component k v)))

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
      (not (nil? explain))
      {:errors [(errors/error ::errors/invalid-view o {:explain-data explain})]}

      (or (symbol? o) (utils/primitive? o)) {:data o}

      (vector? o)
      (let [[element & properties-children]  o
            component                        (resolve-component ctx ext o)
            [properties children]            (resolve-properties-children properties-children)
            {:keys [data errors]}            (when properties
                                               (resolve-component-properties ctx ext element properties))]
        (cond-> (let [m (parse-hiccup-children ctx ext children)]
                  ;; Reduce parsed children to a single map and wrap them in a hiccup element
                  ;; whose component has been translated to the local platform
                  (if component (update m :data #(apply conj (if data [component data] [component]) %)) m))
                (nil? component) (errors/accumulate-errors [(errors/error ::errors/unknown-component element)])
                (seq errors)     (errors/accumulate-errors errors)))
      :else {:errors [(errors/error ::errors/unknown-component o)]})))

(defn unresolved-properties [acc o]
  (cond
    (symbol? o) (conj acc o)
    (vector? o)
    (let [[_ _ & children] o]
      (reduce #(apply conj %1 (unresolved-properties acc %2)) acc children))
    :else acc))

(defn parse [ctx ext o]
  (if (list? o)
    (let [{:keys [data errors]} (blocks/parse ctx ext o)]
      (if data
        (let [d     (parse ctx ext data)
              props (reduce unresolved-properties #{} d)]
          (errors/merge-errors
            d
            (concat errors (when (seq props)
                             {:errors [(errors/error ::errors/unresolved-properties props)]}))))
        {:errors errors}))
    (parse-hiccup-element ctx ext o)))

(defn- hiccup-with-properties [h properties]
  (if (vector? h)
    (let [[tag & properties-children] h
          [props children]            (resolve-properties-children properties-children)
          ;; really only need to add this to the first let block but no harm really
          props (if (and properties (= tag blocks/let-block))
                  (assoc-in props [:prev-env :pluto.reader/properties] properties)
                  props)]
      (apply conj (if props [tag props] [tag])
             (map #(hiccup-with-properties % properties) children)))
    h))

(defmethod types/resolve :view [ctx ext type value]
  (let [{:keys [data errors]} (reference/resolve ctx ext type value)]
    (if data
      (if (fn? data)
        {:data data}
        (let [{:keys [data errors]} (parse ctx ext data)]
          ;; TODO Might fail at runtime if destructuring is incorrect
          (errors/merge-errors (when data {:data (fn [o] (hiccup-with-properties data o))})
                               (concat errors (:errors ext)))))
      {:errors errors})))
