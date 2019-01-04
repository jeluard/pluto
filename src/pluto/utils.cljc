(ns pluto.utils
  (:refer-clojure :exclude [ex-cause ex-message])
  #?(:clj  (:import java.util.Locale))
  (:require [clojure.set         :as set]
            [clojure.string      :as string]
            [pluto.reader.errors :as errors]
            #?(:cljs [goog.string :as gstring])
            #?(:cljs [goog.string.format])))

(defn ex-cause
  [ex]
  #?(:clj  (when (instance? Throwable ex)
             (.getCause ^Throwable ex))
     :cljs (cljs.core/ex-cause ex)))

(defn ex-message
  [ex]
  #?(:clj  (when (instance? Throwable ex)
             (.getMessage ^Throwable ex))
     :cljs (cljs.core/ex-message ex)))

(defn primitive? [o]
  (or (boolean? o)
      (int? o)
      (float? o)
      (string? o)))

(def ^:private placeholder-pattern #"\$\{([^\{]*)\}")

(defn- placeholders
  "Extract a collection of placeholders from a string.
   (placeholders  \"\")"
  [s]
  (map (comp #(hash-map :name (symbol (first %)) :pattern (second %)) #(string/split % #":") second)
       (re-seq placeholder-pattern s)))

(defn- default-format
  "`format` but using `en` locale"
  [pattern]
  (str "%" (or (second (string/split pattern #":")) pattern)))

(defmulti format-pattern
  "Extract the format pattern from the full format.
   Dispatch is done using the last character of the format.

   e.g. format name:5s is dispatched using 's'"
  (fn [pattern] (when (string/includes? pattern ":") (str (last pattern)))))

(defmethod format-pattern "f" [pattern] (default-format pattern))

(defmethod format-pattern "d" [pattern] (default-format pattern))

(defmethod format-pattern "s" [pattern] (default-format pattern))

(defmethod format-pattern :default [_] "%s")

#?(:clj
   (defn- format-en
     "`format` but using `en` locale"
     [fmt & args]
     (String/format (Locale. "en") fmt (to-array args))))

(defn interpolate
  "Interpolates placeholders inside a string.
   Returns an error if a placeholder can't be resolved."
  [values s]
  (let [v     (placeholders s)
        names (map :name v)
        extra (set/difference (set names) (set (keys values)))]
    (if (seq extra)
      {:errors [(errors/error ::errors/invalid-placeholders extra)]}
      {:data
       (if (seq v)
         (apply #?(:clj format-en :cljs gstring/format)
                (string/replace s placeholder-pattern
                                (fn [[_ pattern]] (format-pattern pattern))) (map values names))
         s)})))
