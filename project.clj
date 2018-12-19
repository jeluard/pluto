(defproject status-im/pluto "iteration-4-6-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/tools.reader "1.2.1"]
                 [reagent "0.8.0"]
                 [re-frame "0.10.5"]
                 [binaryage/devtools "0.9.10"]
                 [day8.re-frame/re-frame-10x "0.3.2"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.10"]]

  :source-paths ["src"]

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :none
                                   :cache-analysis false
                                   :target :nodejs
                                   :main "pluto.demo"
                                   :pretty-print true}}
                       {:id "nodejs"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main pluto.js
                                   :output-to "target/nodejs/pluto.js"
                                   :output-dir "target/nodejs/out"
                                   :target :nodejs
                                   :optimizations :advanced
                                   :pretty-print false
                                   :source-map "target/nodejs/reader.map.js"}}                                  
                       {:id                    "pluto.browser"
                        :figwheel               true
                        :source-paths ["src"]
                        :compiler {
                          :main                 pluto.js
                          :target               nodejs
                          :output-to            "target-cljs/cljsbuild/pluto.browser.js"
                          :output-dir           "target-cljs/cljsbuild/js/out/dev"
                          :parallel-build       true 
                          :pretty-print         true
                          :asset-path           "./js/out/dev" 
                          :optimizations        :none
                          :verbose              false
                          :source-map           true
                          :source-map-timestamp true
                          :infer-externs        true
                          :npm-deps             false
                          :recompile-dependents false
                          :foreign-libs [{:file "https://unpkg.com/react@16/umd/react.production.min.js"
                                          :provides       ["react"]
                                          :global-exports {react React}}
                                         {:file "https://unpkg.com/react-dom@16/umd/react-dom.production.min.js"
                                          :provides       ["react-dom"]
                                          :global-exports {react-dom ReactDOM}}
                                         {:file "https://unpkg.com/create-react-class@15.6.0-rc.0/create-react-class.min.js"
                                          :provides       ["create-react-class"]
                                          :global-exports {create-react-class createReactClass}}]}}]})
