.PHONY = tests

compile:
	lein cljsbuild once

install:
	lein install

tests:
	clojure -A:test-clj
	clojure -A:test-cljs
