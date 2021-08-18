dist: cljs-release release

run:
	java -jar target/holdem.jar

cljs-release:
	npx shadow-cljs release web

cljs-build-report:
	npx shadow-cljs run shadow.cljs.build-report web target/shadow-build-report.html

release:
	rm -rf classes
	mkdir classes
	clojure -M:compile
	clojure -M:compile:uberjar

kaocha:
	clojure -M:test:test-runner $(id)

karma:
	npx shadow-cljs compile test
	npx karma start --single-run

coverage:
	clojure -M:test:coverage

ssr:
	npx shadow-cljs release ssr
	node target/index-ssr.js > resources/index-ssr.html
