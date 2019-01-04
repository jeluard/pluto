(ns pluto.reader.block-test
  (:require [clojure.test :refer [is deftest testing]]
            [pluto.core :as reader]
            [pluto.reader.errors :as errors]
            [pluto.reader.blocks :as blocks]
            [re-frame.core :as re-frame])
  #?(:cljs (:require-macros
            [pluto.reader.block-test :refer [with-fetch-data]])))

(deftest let-block
  (testing "parse"
    (is (= {:data [blocks/let-block '{:bindings [s "Hello"]} 's]}
           (blocks/parse {} {} nil '(let [s "Hello"] s))))
    (is (empty?
          (:errors (blocks/parse {:capacities {:queries {'aa {:value :a}}}} {} nil '(let [{a :a} [aa]] a)))))

    (is (= {:data [blocks/let-block
                   '{:bindings [{a :a} {:a {:b 1}} {b :b} a]}
                   'b]}
           (blocks/parse {} {} nil '(let [{a :a} {:a {:b 1}} {b :b} a] b))))
    (is (empty?
         (:errors (blocks/parse {:capacities
                                 {:queries {'aa {:value :a :arguments {:x :string}}}}}
                                {}
                                nil
                                '(let [x 1 {a :a} [aa {:x x}]] a)))))
    
    (is (= {:data [blocks/let-block '{:bindings [s "Hello"]}
                   ['test {} 's]]}
           (blocks/parse {} {} nil (list 'let ['s "Hello"] ['test {} 's]))))
    (is (= (blocks/validate-bindings '[s "Hello" 1])
           [(errors/error ::errors/invalid-bindings-format ['s "Hello" 1])]))

    )

  
  (is (= {:errors [(errors/error ::errors/invalid-bindings-format ['s "Hello" 1])]}
         (blocks/parse {} {} nil (list 'let ['s "Hello" 1] ['test {} 's]))))
  
  (is (= {:data [blocks/let-block '{:bindings [{a :a} {:a 1}]}
                 '[test {} a]]}
         (blocks/parse {} {} nil '(let [{a :a} {:a 1}] [test {} a]))))

)

(deftest let-block-resolution
  (is (= [identity {} 1] (blocks/let-block {:bindings '[a 1] } [identity {} 'a])))
  #_
  (is (= ['test {} 1] (blocks/let-block {:env '{{a :a} [:aa]}} '[test {} a]))))

(defn first-error-type [{:keys [errors]}]
  (-> errors first :pluto.reader.errors/type))

(deftest parse-if-when-errors
  (is (= (first-error-type (blocks/parse {} {} nil '(if [])))
         :pluto.reader.errors/invalid-if-block))
  (is (= (first-error-type (blocks/parse {} {} nil '(if asdf [])))
         :pluto.reader.errors/invalid-if-block))
  (is (= (first-error-type (blocks/parse {} {} nil '(if asdf)))
         :pluto.reader.errors/invalid-if-block))  
  (is (= (first-error-type (blocks/parse {} {} nil '(when [])))
         :pluto.reader.errors/invalid-when-block))
  (is (= (first-error-type (blocks/parse {} {} nil '(when asdf)))
         :pluto.reader.errors/invalid-when-block)))

(declare let-test-capacities)

(deftest resolve-bindings
  (is (= '{a "asdf"
           b "asdf"}
         (blocks/resolve-binding '{a "asdf"} 'b 'a)))
  (is (= '{a {:asdf "foo"}, asdf "foo"}
         (blocks/resolve-binding '{a {:asdf "foo"}} '{asdf :asdf} 'a)))
  (is (= '{:pluto.reader/properties {:asdf "foo"}, asdf "foo"}
         (blocks/resolve-binding
          '{:pluto.reader/properties {:asdf "foo"}} '{asdf :asdf} 'properties)))
  (is (= "asdfg"
         (blocks/resolve-rhs {} '[::identity-query nil {:x "asdfg"}])))

  (is (= "asdfg"
         (blocks/resolve-rhs '{a "asdfg"} '[::identity-query nil {:x a}])))
  
  (is (= '{a "asdf", b "asdf", c "asdf" :hey 1}
         (blocks/resolve-bindings-into {:hey 1} '[a "asdf" b a c b]))))

(deftest resolve-and-validate-queries
  (is (= {:data
          '[a [:pluto.reader.block-test/identity-query nil {:x "asdf"}]
            g "asdf"
            b [:pluto.reader.block-test/identity-map nil {:x "asdf"}]]}

         (blocks/resolve-and-validate-queries
          {:capacities let-test-capacities} {}
          '[a [identity-query {:x "asdf"}]
            g "asdf"
            b [identity-map   {:x "asdf"}]])))

  (is (not-empty (:errors (blocks/resolve-and-validate-queries
        {:capacities let-test-capacities} {}
        '[a [identity-querye {:x "asdf"}]]))))
  
  (is (empty? (:errors (blocks/resolve-and-validate-queries
                        {:capacities let-test-capacities} {}
                        '[a [identity-query {:x a}]])))))

;; The following is all set up so that we can fake "render" the blocks
;; in the resulting view tree

;; this will allow us to verify basic binding replacement behaviors of let blocks

(defn view-component   [& args] [:view args])
(defn text-component   [& args] [:text args])
(defn button-component [& args] [:button args])

(re-frame/reg-sub ::identity-query
                  (fn [db [_ _ {:keys [x]}]] x))

(re-frame/reg-sub ::bool-query
                  (fn [db [_ _ {:keys [x]}]] (= x "true")))

(re-frame/reg-sub ::array-query
                  (fn [db [_ _ {:keys [x y]}]] (cond-> []
                                               x (conj x)
                                               y (conj y))))

(re-frame/reg-sub ::identity-map
                  (fn [db [_ _ {:keys [x]}]] {:asdf x}))

(def fetch-data (atom {}))

(re-frame/reg-sub ::fetch-data
                  (fn [db [_ _ {:keys [id]}]] (get @fetch-data id)))

#?(:clj (defmacro with-fetch-data [data & body]
          `(do (swap! fetch-data merge ~data)
               ~@body)))

(def let-test-capacities
  {:components {'view   {:properties {}
                         :value      view-component}
                'button {:properties {:on-click :event}
                         :value      button-component}
                'text   {:properties {}
                         :value      text-component}}
   :queries '{identity-query {:value ::identity-query :arguments {:x :string}}
              identity-map   {:value ::identity-map :arguments {:x :string}}
              bool-query     {:value ::bool-query :arguments {:x :string}}
              fetch-data     {:value ::fetch-data :arguments {:id :string}}
              array-query    {:value ::array-query :arguments {:x :string :y :string}}}
   :events {'alert
            {:value :alert}}
   :hooks {:main
           {:properties {:view :view}}}})

(defn exec [parsed]
  (cond-> parsed
    (not (:errors parsed))
    (assoc 
     :execed
     ((get-in parsed [:data :hooks :main :demo :parsed :view])
      {:name "test-name-prop"}))))

(defn test-parse [extention]
  (-> (reader/parse {:capacities let-test-capacities} extention)
      exec))

(defn valid-input [val]
  (or (seq? val)
      (nil? val)
      (map? val)
      (vector? val)
      (symbol? val)
      (string? val)
      (number? val)))

; vector-tag | list-of-vector-tags => list-of-vector-tags
(defn simple-render-tree-blocks [current]
  {:pre [(valid-input current)]
   :post [(or (seq? %) (nil? %))]}
  (cond
    (seq? current)
    (mapcat simple-render-tree-blocks current)
    (vector? current)
    (let [[x & xs] current]
      (cond
        (and (fn? x) (#{pluto.reader.blocks/if-block
                        pluto.reader.blocks/when-block
                        pluto.reader.blocks/let-block
                        pluto.reader.blocks/for-block
                        } x))
        (let [new-tree (apply x xs)]
          (simple-render-tree-blocks new-tree))
        :else 
        (list (apply vector x (mapcat simple-render-tree-blocks xs)))))
    (nil? current) current
    :else (list current)))

(defn blocks-render [block-syn]
  (let [{:keys [execed errors] :as res}
        (test-parse (-> '{meta
                          {:name "Test Ext",
                           :description "A test extension",
                           :documentation "Nothing."},
                          hooks/main.demo {:view [main]}}
                        (assoc 'views/main block-syn)))]
    ;; for dev time
    #?(:clj
       (when-not (nil? errors)
         (clojure.pprint/pprint errors)
         (assert (nil? errors))))
    (when (and execed (sequential? execed))
      (simple-render-tree-blocks execed))))

;; end of rendering util to support tesing block rendering

(deftest if-when-block-rendering
  ;; need to set up a query to have a false value?
  (is (= [[view-component "true"]]
         (blocks-render '(let [a "asdf"]
                           (if a
                             [view "true"]
                             [view "false"])))))
  
  (is (= [[view-component "false"]]
       (blocks-render '(let [a [bool-query {:x "false"}]]
                         (if a
                           [view "true"]
                           [view "false"])))))

  (is (= [[view-component "true"]]
         (blocks-render '(let [a [bool-query {:x "true"}]]
                           (if a
                             [view "true"]
                             [view "false"])))))
  
  (is (= [[view-component "true"]]
         (blocks-render '(let [a "asdf"]
                           (when a [view "true"])))))

  (is (= [[view-component]]
         (blocks-render '(let [a [bool-query {:x "false"}]]
                           [view (when a [view "true"])]))))
  
  )

(deftest basic-let-block-replacement []
  (is (= [[view-component "hello"]]
         (blocks-render '(let [a "hello"]
                           [view a]))))
  
  (is (= [[view-component "hello" "jenny"
           [text-component "jenny" "hello"]
           [text-component "hello" "darlene"]]]
         (blocks-render '(let [a "hello"
                               b "jenny"
                               c "darlene"]
                           [view a b
                            [text b a]
                            [text a c]]))))
  (is (= [[view-component "john"]]
         (blocks-render '(let [a "john"
                               b a]
                           [view b]))))
  (is (= [[view-component "john"]]
         (blocks-render '(let [a "john"
                               b a]
                           [view b]))))
  (is (= [[view-component "john"]]
         (blocks-render '(let [a "john"]
                           (let [b a]
                             [view b])))))
  (is (= [[view-component "john"]]
         (blocks-render '(let [a "john"
                               dd a]
                           (let [b a
                                 c b]
                             [view b])))))
  )

(deftest let-blocks-with-properties
  (is (= [[view-component "test-name-prop"]]
         (blocks-render '(let [{name :name} properties]
                           [view name]))))

  (is (= [[view-component "test-name-prop"]]
         (blocks-render '(let [{name :name} properties
                               b name]
                           [view b]))))

  (is (= [[view-component "jolly"]]
         (blocks-render '(let [{name :name} properties
                               b name]
                           (let [name "jolly"]
                             [view name])))))

  (is (= [[view-component "test-name-prop"]]
         (blocks-render '(let [name "jolly"
                               {name :name} properties]
                           [view name]))))
  
  )

(deftest let-blocks-with-queries
  (is (= [[view-component "a temp"]]
         (blocks-render '(let [temp [identity-query {:x "a temp"}]]
                           [view temp]))))

  (is (= [[view-component "a temp" "a temp"]]
         (blocks-render '(let [a "a temp"
                               temp [identity-query {:x a}]]
                           [view a temp]))))

  (is (= [[view-component "a temp" "charmed"]]
         (blocks-render '(let [a "a temp"
                               {asdf :asdf} [identity-map {:x "charmed"}]]
                           [view a asdf]))))
 
  (is (= [[view-component "a temp" "a temp"]]
         (blocks-render '(let [a "a temp"
                               temp [identity-query {:x a}]
                                 ouch temp]
                           [view temp ouch]))))

  (is (= [[view-component "a temp" "a temp"]] 
         (blocks-render '(let [a "a temp"
                               temp [identity-query {:x a}]]
                           (let [ouch temp]
                             [view temp ouch])))))

  (is (= [[view-component "hello"]]
         (with-fetch-data {"data-id" {:foo "hello"}}
           (blocks-render '(let [{foo :foo} [fetch-data {:id "data-id"}]]
                             [view foo])))))
  
  )

(deftest for-block-parse
  (is (= {:data
          [blocks/for-block
           {:bindings '(a [:pluto.reader.block-test/identity-query nil {:x a}])
            :wrapper-component view-component}
           'asdf]}
         (blocks/parse {:capacities let-test-capacities} {} nil
                       '[for [a [identity-query {:x a}]] asdf]))
      ))

(deftest for-blocks
  (is (= [[view-component {} [view-component "foo"] [view-component "bar"]]]
         (blocks-render '(for [a [array-query {:x "foo" :y "bar"}]]
                           [view a]))))

  (is (= [[view-component {} [view-component "foo"] [view-component "bar"]]]
         (blocks-render '(let [b "bar"]
                           (for [a [array-query {:x "foo" :y b}]]
                             [view a])))))

  (is (= [[view-component {} [view-component "foo"] [view-component "bar"]]]
         (blocks-render '(let [b "bar"
                               c [array-query {:x "foo" :y b}]]
                           (for [a c]
                             [view a])))))
  
  (is (= [[view-component {} [view-component "foo"] [view-component "bar"]]]
         (blocks-render '(for [a [array-query {:x "foo" :y "bar"}]]
                           (let [b a]
                             [view b])))))
  
  (with-fetch-data {"for-blocks-data" [{:name "Jane"} {:name "John"} {:name "Sue"}]}

    (is (= [[view-component {}
             [view-component "Jane"]
             [view-component "John"]
             [view-component "Sue"]]]
           (blocks-render
            '(for [{name :name} [fetch-data {:id "for-blocks-data"}]]
               [view name]))))

    (is (= [[view-component {}
             [view-component "Jane"]
             [view-component "John"]
             [view-component "Sue"]]]
           (blocks-render '(for [{name :name} [fetch-data {:id "for-blocks-data"}]]
                             (let [b name]
                               [view b])))))
    
    )

  
  
  )
