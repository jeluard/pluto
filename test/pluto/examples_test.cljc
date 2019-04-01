(ns pluto.examples-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer [is deftest]]
            [pluto.core :as reader]
            #?(:cljs [pluto.utils-test :include-macros true :refer [slurp]])))

#_
(deftest examples
  (is (empty? (:errors (reader/parse {:capacities {:hooks {:main {:properties {}}}}}
                          (:data (reader/read (slurp "examples/resources/public/assets/extensions/demo/extension.edn"))))))))

