(ns pluto.js
  "Exports reader function to JavaScript hosts.
   Type conversion is properly handled."
  (:require [pluto.reader :as reader]))

(defn ^:export to-clj [o]
  (js->clj o))

(defn ^:export from-clj [o]
  (clj->js o))

(defn component [])

;; TODO find a syntax so that :event can define associated types they will be injected

(def ctx
  {:capacities {:components {'view                   {:value component}
                             'scroll-view            {:value component}
                             'keyboard-avoiding-view {:value component}
                             'text                   {:value component}
                             'touchable-opacity      {:value component :properties {:on-press :event}}
                             'icon                   {:value component :properties {:key :keyword :color :any}}
                             'image                  {:value component :properties {:uri :string  :source :number}}
                             'input                  {:value component :properties {:on-change :event :placeholder :string :keyboard-type :keyword :change-delay? :number :placeholder-text-color :any}}
                             'button                 {:value component :properties {:enabled :boolean :disabled :boolean :on-click :event}}
                             'link                   {:value component :properties {:uri :string}}
                             'checkbox               {:value component :properties {:on-change? :event :checked? :boolean}}
                             'activity-indicator     {:value component :properties {:animating :boolean :color :string :size :keyword :hides-when-stopped :boolean}}
                             'list                   {:value component :properties {:data :vector :item-view :view :key? :keyword}}
                             'picker                 {:value component :properties {:on-change :event :selected :string :enabled :boolean :data :vector}}
                             'nft-token-viewer       {:value component :properties {:token :string}}
                             'transaction-status     {:value component :properties {:outgoing :string :tx-hash :string}}
                             'asset-selector         {:value component}
                             'token-selector         {:value component}}
                :queries {'wallet/token {:value :wallet/token :arguments {:token :string}}
                          'wallet/tokens {:value :wallet/tokens :arguments {:filter :vector}}
                          'wallet/balance {:value :wallet/balance :arguments {:token :string}}
                          'wallet/collectibles {:value :get-collectible-token :arguments {:token :string :symbol :string}}
                          'store/get {:value :store/get :arguments {:key :string}}}
                :events     {'alert
                             {:permissions [:read]
                              :value       :alert
                              :arguments   {:value :string}}
                             'selection-screen
                             {:permissions [:read]
                              :value       :extensions/show-selection-screen
                              :arguments   {:items :vector :on-select :event :render :view :title :string :extractor-key :keyword}}
                             'log
                             {:permissions [:read]
                              :value       :log
                              :arguments   {:value :string}}
                             'arithmetic
                             {:permissions [:read]
                              :value       :extensions/arithmetic
                              :arguments   {:values    #{:plus :minus :times :divide}
                                            :operation :keyword
                                            :on-result :event}}
                             'schedule/start
                             {:permissions [:read]
                              :value       :extensions/json-parse
                              :arguments   {:value      :event
                                            :interval   :number
                                            :on-created :event
                                            :on-result  :event}}
                             'schedule/cancel
                             {:permissions [:read]
                              :value       :extensions/json-parse
                              :arguments   {:value      :number}}
                             'json/parse
                             {:permissions [:read]
                              :value       :log
                              :arguments   {:value :string}}
                             'chat.command/set-parameter
                             {:permissions [:read]
                              :value       :store/put
                              :arguments   {:value :string}}
                             'chat.command/set-custom-parameter
                             {:permissions [:read]
                              :value       :store/put
                              :arguments   {:key :keyword :value :string}}
                             'chat.command/set-parameter-with-custom-params
                             {:permissions [:read]
                              :value       :store/put
                              :arguments   {:value :string :params :map}}
                             'chat.command/send-message
                             {:permissions [:read]
                              :value       :chat.command/send-message
                              :arguments   {:params :map}}
                             'chat.command/send-plain-text-message
                             {:permissions [:read]
                              :value       :chat.command/send-message
                              :arguments   {:value :string}}
                             'store/put
                             {:permissions [:read]
                              :value       :store/put
                              :arguments   {:key :string :value :any}}
                             'store/puts
                             {:permissions [:read]
                              :value       :store/puts
                              :arguments   {:value :vector}}
                             'store/append
                             {:permissions [:read]
                              :value       :store/append
                              :arguments   {:key :string :value :any}}
                             'store/clear
                             {:permissions [:read]
                              :value       :store/put
                              :arguments   {:key :string}}
                             'http/get
                             {:permissions [:read]
                              :value       :http/get
                              :arguments   {:url         :string
                                            :timeout?    :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'http/post
                             {:permissions [:read]
                              :value       :http/post
                              :arguments   {:url         :string
                                            :body        :string
                                            :timeout?    :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ipfs/cat
                             {:permissions [:read]
                              :value       :ipfs/cat
                              :arguments   {:hash        :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/transaction-receipt
                             {:permissions [:read]
                              :value       :extensions/ethereum-transaction-receipt
                              :arguments   {:value     :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/send-transaction
                             {:permissions [:read]
                              :value       :extensions/ethereum-send-transaction
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
                              :value       :extensions/ethereum-logs
                              :arguments   {:fromBlock? :string
                                            :toBlock?   :string
                                            :address?   :vector
                                            :topics?    :vector
                                            :blockhash? :string
                                            :on-success  :event
                                            :on-failure? :event}}
                             'ethereum/call
                             {:permissions [:read]
                              :value       :extensions/ethereum-call
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
  (reader/parse ctx (:data m)))
