(ns pluto.reader.views-test
  (:refer-clojure :exclude [resolve])
  (:require [clojure.test :refer [is deftest testing]]
            [pluto.reader.blocks :as blocks]
            [pluto.reader.errors :as errors]
            [pluto.reader.types  :as types]
            [pluto.reader.views  :as views]))

(deftest parse-hiccup-children
  (is (=  {:data (list [:text {} ""])}
          (views/parse-hiccup-children {:capacities {:components {'text {:value :text}}}}
                                       {}
                                       (list ['text {} ""])))))

(defn- first-error-type [m]
  (::errors/type (first (:errors m))))

(deftest parse
  #_
  (is (= ::errors/invalid-view (first-error-type (views/parse {} {}))))
  #_
  (is (= ::errors/invalid-view
         (first-error-type (views/parse {:capacities {:components {'text {:value :text}}}} ['text "Hello"]))))
  #_
  (is (= ::errors/invalid-view
         (first-error-type (views/parse {:capacities {:components {'text {:value :text}}}} ['text {} []]))))
  #_
  (is (= {:data   ['text {} "Hello"]
          :errors (list {::errors/type ::errors/unknown-component ::errors/value 'text})}
         (views/parse {} ['text {} "Hello"])))
  (is (= {:data [:text {} "Hello"]}
         (views/parse {:capacities {:components {'text {:value :text}}}} {} ['text {} "Hello"])))
  (is (= {:data [:text {} "Hello"]}
         (views/parse {:capacities {:components {'text {:value :text}}}} {} ['text {} "Hello"])))
  (is (= {:data [:view
                  [:text {} "Hello"]
                  [blocks/let-block
                   {:env '{cond? (query [:random-boolean])}}
                   [blocks/if-block
                     {:test 'cond?}
                     [:text {:style {:color "green"}} "World?"]
                     [:text {:style {:color "red"}} "World?"]]]]}
         (views/parse {:capacities {:queries #{:random-boolean}
                                    :components {'text {:value :text}
                                                 'view {:value :view}}}}
                      {}
                      '[view
                        [text {} "Hello"]
                        (let [cond? (query [:random-boolean])]
                          (if cond?
                            [text {:style {:color "green"}}
                             "World?"]
                            [text {:style {:color "red"}}
                             "World?"]))])))
  (testing "Properties"
    (is (= {:data [:text {} "Hello"]}
           (views/parse {:capacities {:components {'text {:value :text}}}} {} ['text {} "Hello"])))))

(deftest resolve
  (is (= [:text "Hello"] ((:data (types/resolve {:capacities {:components {'text {:value :text}}}} {'views/main ['text "Hello"]} :view ['views/main])) {})))
  (is (= {:errors [{::errors/type  ::errors/unknown-reference,
                    ::errors/value {:value 'views/unknown}}]}
         (types/resolve {:capacities {:components {'text {:value :text}}}} {'views/main ['text "Hello"]} :view ['views/unknown]))))

(deftest invalid-view-element-spec-errors
  (letfn [(p [view] (views/parse
                     {:capacities {:components {'text {:properties {:asdf :string}
                                                       :value :text}}}}
                     {}
                     view))]
    (is (= (first-error-type (p '[text :sadf]))
           :pluto.reader.errors/invalid-view))
    (is (= (first-error-type (p '[text {} {}]))
           :pluto.reader.errors/invalid-view))
    
    (is (not (:errors (p '[text [text]]))))
    (is (not (:errors (p '[text {} 1 2 3 4 asdf]))))

    (is (= (first-error-type (p '[text {asdf "asdf"}]))
           :pluto.reader.errors/invalid-property-map))))

