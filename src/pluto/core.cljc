(ns pluto.core
  "Main pluto namespace entry point."
  (:refer-clojure :exclude [read])
  (:require [clojure.string           :as string]
            [clojure.tools.reader.edn :as edn]
            [pluto.reader.errors      :as errors]
            [pluto.reader.events      :as events]
            [pluto.reader.types       :as types]
            [pluto.reader.views       :as views]
            [pluto.utils              :as utils]))

(defn- reader-error [ex]
  (errors/error ::errors/reader-error (:ex-kind (ex-data ex))
                (merge {::errors/message (utils/ex-message ex)}
                       (when-let [c (utils/ex-cause ex)]
                         {:cause c}))))

(defn read
  "Reads an extension definition as an EDN string.

   No semantic validation is performed at this stage.

   Returns a map defining:
   * `:data` the extension definition as a map
   * `:errors` a vector of errors map triggered during read"
  [s]
  (try
    {:data (edn/read-string {} s)}
    (catch #?(:clj Exception :cljs :default) ex
      {:errors [(reader-error ex)]})))

(defmulti parse-value
  "Parse an extension value from its type"
  (fn [ctx ext k v] (namespace k)))

(defmulti parse-value
  "Parse an extension value from its type"
  (fn [ctx ext k v] (namespace k)))

(defn- capacity? [m s]
  (let [keys (set (map name (keys m)))]
    (keys (name s))))

(defn parse-value-with [capacities t k f]
  (if (capacity? (get capacities t) k)
    [(errors/error ::errors/existing-key k)]
    (f)))

(defmethod parse-value "views" [{:keys [capacities] :as ctx} ext k o]
  (parse-value-with capacities :components k #(views/parse ctx ext o)))

(defmethod parse-value "events" [{:keys [capacities] :as ctx} ext k o]
  (parse-value-with capacities :events k #(events/parse ctx ext o "")))

(defn- hook-type
  "Type of a hook
   e.g. (= \"chat.command\" (hook-type 'chat.command.hello-world))"
  [s]
  (when s
    (string/join "." (butlast (string/split (name s) #"\.")))))

(defmethod parse-value "hooks" [{:keys [capacities] :as ctx} ext k o]
  (parse-value-with capacities :hooks k
                    #(let [{:keys [properties]} (get-in ctx [:capacities :hooks (keyword (hook-type k))])]
                       (types/resolve ctx ext properties o))))

(defmethod parse-value :default [_ _ k _]
  [(errors/error ::errors/invalid-key k)])

(defn- accumulate
  "Accumulates the result of parsed primitives.
   Shape is: {:data {'primitive data} :errors {'primitive errors} :permissions #{}}
   If returned map contains :errors, :data is ignored."
  [ctx ext acc k v]
  (let [{:keys [data errors]} (parse-value ctx ext k v)]
    (assoc-in acc [(if errors :errors :data) (keyword (namespace k)) (keyword (name k))] (or errors data))))

(def ^:const order ["events" "queries" "views" "hooks"])

(defn- order-comparator
  "Compares keys based on `order`"
  [k1 k2]
  (let [indexes (zipmap order (range))]
    (compare [(get indexes (namespace k1)) k1] [(get indexes (namespace k2)) k2])))

;; TODO somehow hook re-frame loggers into :logger
;; https://github.com/Day8/re-frame/blob/master/docs/FAQs/Logging.md

(defn parse
  "Parse an extension definition map as encapsulated in :data key of the map returned by `read`.
   `ctx` is a map defining:
   * `capacities` a map of valid supported capacities (hooks, queries, events)
   * `env`        a map of extension environment, will be provided as second parameter into event and query handlers

   `env` is a map defining:
   * `logger`     [optional] a function that will be passed details about runtime extension execution (event fired, query values updated, ..): {:type :event :name 'my-event :properties {}}

   Returns the input map modified so that values have been parsed into:
   * `:data`        the result of parsing
   * `:permissions` a vector of required permissions
   * `:errors`      a vector of errors maps triggered during the parsing

   If `errors` is not empty `data` will not be available.

   e.g.

   {'view/a {:data ..
             :permissions ..}

    'view/b {:errors []}}

   or

   {:data        {'view/a (fn [o] [text \"hello\"])}
    :permissions {'view/a #{}}"
  [ctx ext]
  (reduce-kv #(accumulate ctx ext %1 %2 %3) {} ;; TODO move ext to %1
             ;; Make sure elements are parsed in a controlled order
             (into (sorted-map-by order-comparator) (dissoc ext 'meta))))
