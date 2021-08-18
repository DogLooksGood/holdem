(ns user
  (:require
   [poker.system.config  :as config]
   [poker.system.db      :as db]
   [poker.system.web-server :as web-server]
   [poker.system.game-registry :as game-registry]
   [poker.system.web-pub :as web-pub]
   [mount.core           :as mount]))

(defn start
  []
  (mount/start #'config/config
               #'db/node
               #'web-server/web-server
               #'web-pub/pub-bus
               #'web-server/websocket-event-loop
               #'game-registry/registry))

(defn stop
  []
  (mount/stop))

(comment
  (start)
  (stop))
