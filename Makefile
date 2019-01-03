.PHONY = tests

dev:
	clojure -A:examples

tests:
	clojure -A:test-clj
	clojure -A:test-cljs
