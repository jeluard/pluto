(ns pluto.reader.events-test
  (:refer-clojure :exclude [resolve])
  (:require [clojure.test :refer [is deftest testing]]
            [pluto.reader.events :as events]))

(deftest local-event?
  (is (events/local-event? '(let [{} properties] [alert {}]))))
