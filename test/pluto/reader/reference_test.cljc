(ns pluto.reader.reference-test
  (:refer-clojure :exclude [resolve])
  (:require [clojure.test :refer [is deftest]]
            [pluto.reader.errors    :as errors]
            [pluto.reader.reference :as reference]))

(deftest valid-reference?
  (is (false? (reference/reference? nil)))
  (is (false? (reference/reference? "")))
  (is (false? (reference/reference? 'test)))
  (is (false? (reference/reference? :keyword)))
  (is (true?  (reference/reference? ['test])))
  (is (true?  (reference/reference? ['views/id {}])))
  (is (false? (reference/reference? ['views/id {} {}])))
  (is (false? (reference/reference? ['views/id 1 {}])))
  (is (false? (reference/reference? ["views/id" {}])))
  (is (true?  (reference/reference? ['views/id {}])))
  (is (true?  (reference/reference? ['views/id 'arg]))))

(deftest reference->symbol
  (is (= nil (reference/reference->symbol "")))
  (is (= 'test (reference/reference->symbol ['test])))
  (is (= 'views/id (reference/reference->symbol ['views/id {}]))))

(deftest resolve
  (is (= {:errors [{::errors/type  ::errors/unknown-reference
                    ::errors/value {:value 'id :type :view}}]}
         (reference/resolve {} {} :view ['id])))
  (is (= {:errors [{::errors/type  ::errors/invalid-reference
                    ::errors/value {:value "" :type :view}}]}
         (reference/resolve {} {'views/id "view"} :view "")))
  (is (= {:errors [{::errors/type  ::errors/unknown-reference-type
                    ::errors/value {:value :unknown}}]}
         (reference/resolve {} {'views/id "view"} :unknown ['id])))
  (is (= {:data "view"}
         (reference/resolve {} {'views/id "view"} :view ['id])))
  (is (= {:data :div}
         (reference/resolve {:capacities {:components {'component {:value :div}}}} {} :view ['component]))))
