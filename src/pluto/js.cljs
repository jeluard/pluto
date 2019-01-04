(ns pluto.js
  "Exports reader function to JavaScript hosts.
   Type conversion is properly handled."
  (:require [pluto.core :as pluto]))

(defn ^:export to-clj [o]
  (js->clj o))

(defn ^:export from-clj [o]
  (clj->js o))

(defn component [])

;; TODO find a syntax so that :event can define associated types they will be injected

(def ctx
  {:capacities {:components {'view                   {:data component}
                             'scroll-view            {:data component}
                             'keyboard-avoiding-view {:data component}
                             'text                   {:data component}
                             'touchable-opacity      {:data component :properties {:on-press :event}}
                             'icon                   {:data component :properties {:key :keyword :color :any}}
                             'image                  {:data component :properties {:uri :string  :source :number}}
                             'input                  {:data component :properties {:on-change :event :placeholder :string :keyboard-type :keyword :change-delay? :number :placeholder-text-color :any}}
                             'button                 {:data component :properties {:enabled :boolean :disabled :boolean :on-click :event}}
                             'link                   {:data component :properties {:uri :string}}
                             'checkbox               {:data component :properties {:on-change? :event :checked? :boolean}}
                             'activity-indicator     {:data component :properties {:animating :boolean :color :string :size :keyword :hides-when-stopped :boolean}}
                             'list                   {:data component :properties {:data :vector :item-view :view :key? :keyword}}
                             'picker                 {:data component :properties {:on-change :event :selected :string :enabled :boolean :data :vector}}
                             'nft-token-viewer       {:data component :properties {:token :string}}
                             'transaction-status     {:data component :properties {:outgoing :string :tx-hash :string}}
                             'asset-selector         {:data component}
                             'token-selector         {:data component}}
                :queries {'wallet/token {:data :wallet/token :arguments {:token :string}}
                          'wallet/tokens {:data :wallet/tokens :arguments {:filter :vector}}
                          'wallet/balance {:data :wallet/balance :arguments {:token :string}}
                          'wallet/collectibles {:data :get-collectible-token :arguments {:token :string :symbol :string}}
                          'store/get {:data :store/get :arguments {:key :string}}}
                :events     {'alert
                             {:permissions [:read]
                              :data       :alert
                              :arguments   {:data :string}}
                             'selection-screen
                             {:permissions [:read]
                              :data       :extensions/show-selection-screen
                              :arguments   {:items :vector :on-select :event :render :view :title :string :extractor-key :keyword}}
                             'log
                             {:permissions [:read]
                              :data       :log
                              :arguments   {:data :string}}
                             'arithmetic
                             {:permissions [:read]
                              :data       :extensions/arithmetic
                              :arguments   {:values    :vector
                                            :operation {:one-of #{:plus :minus :times :divide}}
                                            :on-result :event}}
                             'schedule/start
                             {:permissions [:read]
                              :data       :extensions/json-parse
                              :arguments   {:value      :event
                                            :interval   :number
                                            :on-created :event
                                            :on-result  :event}}
                             'schedule/cancel
                             {:permissions [:read]
                              :data       :extensions/json-parse
                              :arguments   {:value      :number}}
                             'json/parse
                             {:permissions [:read]
                              :data       :log
                              :arguments   {:value :string}}
                             'chat.command/set-parameter
                             {:permissions [:read]
                              :data        :store/put
                              :arguments   {:value :string}}
                             'chat.command/set-custom-parameter
                             {:permissions [:read]
                              :data        :store/put
                              :arguments   {:key :keyword :value :string}}
                             'chat.command/set-parameter-with-custom-params
                             {:permissions [:read]
                              :data        :store/put
                              :arguments   {:value :string :params :map}}
                             'chat.command/send-message
                             {:permissions [:read]
                              :data        :chat.command/send-message
                              :arguments   {:params :map}}
                             'chat.command/send-plain-text-message
                             {:permissions [:read]
                              :data        :chat.command/send-message
                              :arguments   {:value :string}}
                             'store/put
                             {:permissions [:read]
                              :data        :store/put
                              :arguments   {:key :string :value :any}}
                             'store/puts
                             {:permissions [:read]
                              :data        :store/puts
                              :arguments   {:value :vector}}
                             'store/append
                             {:permissions [:read]
                              :data        :store/append
                              :arguments   {:key :string :value :any}}
                             'store/clear
                             {:permissions [:read]
                              :data        :store/put
                              :arguments   {:key :string}}
                             'http/get
                             {:permissions [:read]
                              :data        :http/get
                              :arguments   {:url         :string
                                            :timeout?    :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'http/post
                             {:permissions [:read]
                              :data        :http/post
                              :arguments   {:url         :string
                                            :body        :string
                                            :timeout?    :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ipfs/cat
                             {:permissions [:read]
                              :data        :ipfs/cat
                              :arguments   {:hash        :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/transaction-receipt
                             {:permissions [:read]
                              :data        :extensions/ethereum-transaction-receipt
                              :arguments   {:value     :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/send-transaction
                             {:permissions [:read]
                              :data        :extensions/ethereum-send-transaction
                              :arguments   {:to         :string
                                            :gas?       :string
                                            :gas-price? :string
                                            :value?     :string
                                            :method?    :string
                                            :params?    :vector
                                            :nonce?     :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/logs
                             {:permissions [:read]
                              :data        :extensions/ethereum-logs
                              :arguments   {:fromBlock? :string
                                            :toBlock?   :string
                                            :address?   :vector
                                            :topics?    :vector
                                            :blockhash? :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/call
                             {:permissions [:read]
                              :data        :extensions/ethereum-call
                              :arguments   {:to         :string
                                            :method     :string
                                            :params?    :vector
                                            :on-success  :event
                                            :on-failure? :event}}}
                :hooks {:wallet.settings {:properties {:label     :string
                                                       :view      :view
                                                       :on-click? :event}}
                        :chat.command {:properties {:description?  :string
                                                    :scope         #{:personal-chats :public-chats}
                                                    :short-preview :view
                                                    :preview       :view
                                                    :parameters?    [{:id           :keyword
                                                                      :type         {:one-of #{:text :phone :password :number}}
                                                                      :placeholder  :string
                                                                      :suggestions? :view}]
                                                    :on-send?      :event
                                                    :on-send-sync? :event
                                                    :on-receive?   :event}}}}})

(defn ^:export parse [m]
  (pluto/parse ctx (:data m)))
