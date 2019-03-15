(ns pluto.reader.views
  (:require [clojure.set            :as set]
            [clojure.spec.alpha     :as spec]
            #?(:cljs [reagent.core  :as reagent])
            [pluto.reader.blocks    :as blocks]
            [pluto.reader.errors    :as errors]
            [pluto.reader.reference :as reference]
            [pluto.reader.types     :as types]
            [pluto.utils            :as utils]
            [pluto.trace            :as trace]))

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

(defn parse-hiccup-children [ctx ext parent children]
  (reduce #(let [{:keys [data errors]} (parse ctx ext parent %2)]
             (errors/merge-errors (update %1 :data conj data)
                                  errors))
          {:data []} children))

(defn component? [o]
  (symbol? o))

(defn- block? [o]
  ;; TODO better abstract blocks
  (fn? o))

(defn- resolve-component [ctx ext [element :as o]]
  (cond
    (block? element) element
    (symbol? element) (or (get-in ctx [:capacities :components element :data])
                          ; First resolve using default components then lookup for local views
                          ;; TODO handle errors
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

(defn parse-hiccup-element [ctx ext o]
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
        (cond-> (let [m (parse-hiccup-children ctx ext o children)]
                  ;; Reduce parsed children to a single map and wrap them in a hiccup element
                  ;; whose component has been translated to the local platform
                  (if component (update m :data #(apply conj (if data [component data] [component]) %)) m))
                (nil? component) (errors/accumulate-errors [(errors/error ::errors/unknown-component element)])
                (seq errors)     (errors/accumulate-errors errors)))
      :else {:errors [(errors/error ::errors/unknown-component o)]})))

(defn unresolved-properties [acc o]
  (cond
    (symbol? o) (conj acc o)
    (map? o)    (reduce #(apply conj %1 (unresolved-properties acc %2)) acc (vals o))
    (vector? o) (reduce #(apply conj %1 (unresolved-properties acc %2)) acc o)
    :else acc))

(defn event->fn [ctx ext event f]
  (fn [& o]
    (when event
      (let [{:keys [data errors]} (types/resolve ctx ext :event event)]
        (when data
          (data (apply f o)))))))

#?(:cljs
    (defn default-logger [ctx error info]
      (trace/trace ctx (trace/create-trace :error :view {:error error :info info}))))

(defn error-boundary [ctx component]
  #?(:cljs
      (reagent/create-class
        {:display-name        "error-boundary-wrapper"
         :component-did-catch #(default-logger ctx %1 %2)
         :reagent-render      (fn error-boundary [_] component)})))

(defn- inject-properties
  "Inject `properties` into the top level `let` block."
  ;; TODO remove this dependency on specifics of let block
  [h properties]
  (if (vector? h)
    (let [[tag & properties-children] h
          [props children]            (resolve-properties-children properties-children)
          ;; Only need to add this to the first let block but no harm really
          props (if (and properties (= tag blocks/let-block))
                  (assoc-in props [:prev-env :pluto.reader/properties] properties)
                  props)]
      (apply conj (if props [tag props] [tag])
        (map #(inject-properties % properties) children)))
    h))

#?(:cljs
    (defn- create-reagent-spec [ctx ext {:keys [get-initial-state component-will-receive-props should-component-update
                                                component-will-mount component-did-mount component-will-update
                                                component-did-update component-will-unmount]} data]
      (merge {:display-name        (str (first data))
              :reagent-render      (fn [o]
                                     [error-boundary ctx
                                      (inject-properties data o)])}
        (when get-initial-state {:get-initial-state-mount (event->fn ctx ext get-initial-state #(js->clj %))})
        (when component-will-receive-props {:component-will-receive-props (event->fn ctx ext component-will-receive-props #(assoc (js->clj %1) :new %2))})
        (when should-component-update {:should-component-update (event->fn ctx ext should-component-update #(assoc (js->clj %1) :old %2 :new %3))})
        (when component-will-mount {:component-will-mount (event->fn ctx ext component-will-mount #(js->clj %))})
        (when component-did-mount {:component-did-mount (event->fn ctx ext component-did-mount #(do {}))})
        (when component-will-update {:component-will-update (event->fn ctx ext component-will-update #(assoc (js->clj %1) :new %2))})
        (when component-did-update {:component-did-update (event->fn ctx ext component-did-update #(assoc (js->clj %1) :old %2))})
        (when component-will-unmount {:component-will-unmount (event->fn ctx ext component-will-unmount #(do {}))}))))

(defn bindings [data]
  (let [o (first (get-in data [1 :bindings]))]
    ;; TODO Follow symbols
    (when (map? o)
      (set (keys o))))) ;; TODO extract all bindings props, not only first level

;; TODO normalize to always have a props map
(defn parse
  ([ctx ext o]
   (let [{:keys [data errors] :as m} (parse ctx ext nil (if (map? o) (:view o) o))]
     (if errors
       m
       (if (map? o)
         #?(:cljs {:data (reagent/create-class (create-reagent-spec ctx ext o data))})
         {:data
          (fn [o]
            [error-boundary ctx
             (inject-properties data o)])}))))
  ([{:keys [view-fn] :as ctx} ext {:keys [path] :as parent-ctx} o]
   ((if view-fn #(view-fn parent-ctx %) identity)
    (if (list? o)
      (let [{:keys [data errors]} (blocks/parse ctx ext parent-ctx o)]
        (if errors
          {:errors errors}
          (let [d     (parse ctx ext {:parent o :path (conj path 0)} data)
                ;; TODO Properly introduce `bindings` at top parsing level, not in blocks
                props (set/difference (reduce unresolved-properties #{} d)
                         (bindings data))]
            (errors/merge-errors
              d
              (concat errors (when (seq props)
                               {:errors [(errors/error ::errors/unresolved-properties props)]}))))))
      (parse-hiccup-element ctx ext parent-ctx o)))))

(defmethod types/resolve :view [ctx ext type value]
  (let [{:keys [data errors]} (reference/resolve ctx ext type value)]
    (if data
      (parse ctx ext data)
      {:errors errors})))
