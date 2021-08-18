(ns poker.main
  (:gen-class)
  (:require
   [poker.system.config     :as config]
   [poker.system.db         :as db]
   [poker.system.web-server :as web-server]
   [poker.system.game-registry :as game-registry]
   [poker.system.web-pub    :as web-pub]
   [mount.core              :as mount]
   [clojure.tools.logging   :as log]))

(set! *warn-on-reflection* true)

(defn -main
  [& _args]
  (mount/start #'config/config
               #'db/node
               #'web-server/web-server
               #'web-pub/pub-bus
               #'web-server/websocket-event-loop
               #'game-registry/registry)
  (log/info "Application started"))
