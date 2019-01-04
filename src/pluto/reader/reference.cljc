(ns pluto.reader.reference
  "Utils functions helping with primitive references.

   A reference is a vector whose first element is a symbol and optional second element is a map.

   e.g. [view] or [ethereum/log {:address \"\"}]"
  (:refer-clojure :exclude [resolve])
  (:require [pluto.reader.errors :as errors]))

(defn reference?
  "Return true if argument is a reference"
  [ref]
  (when (vector? ref)
    (let [[name arguments] ref]
      (and (symbol? name)
           (>= 2 (count ref))
           (or (nil? arguments) (map? arguments) (symbol? arguments))))))

(defn reference->symbol
  "Return the symbol pointed by the reference

   ```clojure
   (= 'some.ref (reference->symbol ['some.ref]))
   ```"
  [o]
  (when (reference? o)
    (first o)))

(def ^:const type->ns  {:view "views" :query "queries" :event "events"})
(def ^:const type->capacity {:view :components :query :queries :event :events})

(defn- resolve-symbol
  "Resolve a symbol first via the extension definition then via the host ctx."
  [ctx ext type ns s]
  (or (get ext (symbol ns (name s)))
      (get-in ctx [:capacities (get type->capacity type) s :data])))

(defn resolve
  "Resolve a reference to a primitive.

   ```clojure
   (= {:data \"view\"} (resolve {} {'views/id \"view\"} :view ['id]))
   ```"
  [ctx ext type ref]
  (if-let [s (reference->symbol ref)]
    (if-let [ns (get type->ns type)]
      (if-let [o (resolve-symbol ctx ext type ns s)]
        {:data o}
        {:errors [(errors/error ::errors/unknown-reference {:value s :type type})]})
      {:errors [(errors/error ::errors/unknown-reference-type {:value type})]})
    {:errors [(errors/error ::errors/invalid-reference {:type type :value ref})]}))
