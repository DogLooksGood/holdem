# Holdem

A lightweight Texas Hold'em Poker game, WIP.

## Install requirements
```sh
clojure -P
npm i
```

## Development
Start Clojure/Script REPL via `cider-jack-in-clj&cljs` in Emacs.

Start backend system by type `(start)` in Clojure REPL.

WEB services will be available at `http://localhost:4000`.

Workspace cards will be available at `http://localhost:8000/workspaces.html`.

Shadow CLJS UI is `http://localhost:9630`.

## Release

- `make cljs-release` will compile ClojureScript
- `make release` will compile Clojure and generate uberjar

```
make
```

Uberjar will be generated at `target/holdem.jar`, run with:

```
make run
```

or

```
java -jar target/holdem.jar
```

Open http://localhost:4000 in your browser.

## Testing
### Backend
Run backend tests with kaocha. The configuartion is tests.edn.
```
make kaocha
```

### Frontend
Run frontend tests with karma.
```
make karma
```
