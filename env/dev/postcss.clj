(ns postcss
  (:require
   [babashka.process :as proc]))

(defn watch
  {:shadow.build/stage :configure}
  [build-state src dst]
  (proc/process ["./node_modules/.bin/postcss" src "-o" dst "--verbose" "-w"]
                {:env {"TAILWIND_MODE" "watch"}})
  build-state)

(defn release
  {:shadow.build/stage :configure}
  [build-state src dst]
  (-> (proc/process ["./node_modules/.bin/postcss" src "-o" dst "--verbose"]
        {:env {"NODE_MODE" "production"}})
    (proc/check))
  build-state)
