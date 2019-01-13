(ns pluto.reader.blocks
  (:require [clojure.walk               :as walk]
            [re-frame.core              :as re-frame]
            [pluto.reader.destructuring :as destructuring]
            [pluto.reader.errors        :as errors]
            [pluto.reader.types         :as types]
            [pluto.utils                :as utils]))

(defn block? [o]
      (and (list? o)
           (symbol? (first o))
           (map? (second o))))

(defmulti parse
  "Parse a block element. Return hiccup data."
  (fn [ctx ext parent [type]] type))

(defn substitute-query-values [m v]
  (walk/prewalk #(or (get m %) (when (string? %) (:data (utils/interpolate m %))) %) v))

(defn- query? [binding-value]
  (and (vector? binding-value)
       (let [s (first binding-value)]
         (or (symbol? s) (keyword? s)))))

(defn resolve-rhs [env v]
  (cond
    (= v 'properties) (get env :pluto.reader/properties)
    (symbol? v) (get env v)
    (query? v) (some-> (re-frame/subscribe (substitute-query-values env v)) deref)
    :else v))

(defn destructure-into [env k v]
  (if (map? k)
    (into env (:data (destructuring/destructure k v)))
    (assoc env k v)))

(defn resolve-binding [env k v]
  (let [v' (resolve-rhs env v)]
    (destructure-into env k v')))

(defn resolve-bindings-into [env bindings]
  (reduce #(apply resolve-binding %1 %2) (or env {}) (partition 2 bindings)))

(defn replace-atom [values o]
  (cond (contains? values o) (get values o)
        (symbol? o) nil
        (string? o) (:data (utils/interpolate values o))
        (and (fn? o) (:event (meta o))) #(o % values) ;; Intercept events and inject the env. TODO remove this hack
        :else (walk/postwalk-replace values o)))

(defn walkup-upto-leaf [f lp? lf tree]
  (if (lp? tree)
    (lf tree)
    (let [res (f tree)
          f2 (partial walkup-upto-leaf f lp? lf)]
      (cond (list? res) (apply list (map f2 res))
            (map-entry? res) (vec (map f2 res))
            (seq? res) (doall (map f2 res))
            (coll? res) (into (empty res) (map f2 res))
            :else res))))

(declare let-block for-block)

(defn let-block [{:keys [prev-env bindings]} children]
  (let [new-env (resolve-bindings-into prev-env bindings)]
    (walkup-upto-leaf #(replace-atom new-env %)
                      #(and (vector? %) (#{for-block let-block} (first %)))
                      (fn [[x props children]]
                        [x (assoc props :prev-env new-env) children])
                      children)))

(defn for-block [{:keys [prev-env bindings]} children]
  (let [[k v] bindings
        for-values (resolve-rhs prev-env v)]
    (when (sequential? for-values)
      (into (if true [:<> {}] (list))
            (for [val for-values]
              (let-block {:prev-env prev-env :bindings [k val]}
                children))))))

(defn static-value? [v]
  (or (utils/primitive? v) (map? v)))

(defn valid-bindings? [k v]
  (and (or (symbol? k) (map? k))
       (or (symbol? v) (static-value? v) (query? v))))

(defn- valid-bindings-form? [bindings]
  (even? (count bindings)))

(defn resolve-and-validate-queries [ctx ext bindings]
  (reduce (fn [accum [k v]]
            (if (vector? v)
              (let [{:keys [data errors]} (types/resolve ctx ext :query v)]
                (if (not-empty errors)
                  (update accum :errors concat errors)
                  (update accum :data concat [k data])))
              (update accum :data concat [k v])))
          {:data []}
          (partition 2 bindings)))

;; we also need a set of available symbols bound at this point
(defn validate-bindings [bindings]
  (if (valid-bindings-form? bindings)
    (not-empty
     (let [binding-pairs (partition 2 bindings)]
       (concat
        (->> binding-pairs
             (filter #(not (apply valid-bindings? %)))
             (mapv #(errors/error ::errors/invalid-bindings %)))
        (->> binding-pairs
             (map first)
             (filter (some-fn sequential? map?))
             (mapcat destructuring/validate-destructure-bindings)))))
    [(errors/error ::errors/invalid-bindings-format bindings)]))

(defn- valid-let-block? [body]
  (= 1 (count body)))

(defmethod parse 'let [ctx ext parent [_ bindings & body]]
  (if-not (valid-let-block? body)
    {:errors [(errors/error ::errors/invalid-let-body {:value body})]}
    (let [binding-errors (validate-bindings bindings)]
      (if (not-empty binding-errors)
        {:errors binding-errors}
        (let [{:keys [errors data]} (resolve-and-validate-queries ctx ext bindings)]
          (if (not-empty errors)
            {:errors errors}
            {:data [let-block {:bindings data} (last body)]}))))))

(defmethod parse 'for [ctx ext parent [_ binding & body]]
  (cond
    (not= 1 (count body))
    {:errors [(errors/error ::errors/invalid-for-body body)]}
    (or (not= 2 (count binding))
        (not ((some-fn symbol? map?) (first binding))))
    {:errors [(errors/error ::errors/invalid-for-binding binding)]}
    :else
    (let [{:keys [errors data]} (resolve-and-validate-queries ctx ext binding)]
      (if (not-empty errors)
        {:errors errors}
        {:data [for-block {:bindings data}
                (last body)]}))))

(defn when-block [{:keys [test]} body]
  (when test body))

(defmethod parse 'when [_ _ _ [_ test & body :as parts]]
  (let [errors (cond-> nil
                 (not (symbol? test))
                 (conj (errors/error ::errors/unsupported-test-type test))
                 (empty? body)
                 (conj (errors/error ::errors/invalid-when-block parts {:empty-body-clause body})))]
    (if (not-empty errors)
      {:errors errors}
      {:data (apply conj [when-block {:test test}] body)})))

(defn if-block [{:keys [test]} & body]
  (if test
    (first body)
    (second body)))

(defmethod parse 'if [_ _ _ [_ test then else :as parts]]
  (let [parts-count (count (rest parts))
        errors (cond-> nil
                 (not (symbol? test))
                 (conj (errors/error ::errors/unsupported-test-type test))
                 (< 3 parts-count)
                 (conj (errors/error ::errors/invalid-if-block parts {:type :too-many-clauses
                                                                      :clause-count parts-count}))
                 (> 3 parts-count)
                 (conj (errors/error ::errors/invalid-if-block parts {:type :three-clauses-required
                                                                      :clause-count parts-count})))]
    (if (not-empty errors)
      {:errors errors}
      {:data (apply conj [if-block {:test test}] (list then else))})))

(defmethod parse :default [ctx _ block] {:errors [{:type :unknown-block-type :ctx ctx :block block}]})
