(ns poker.system.game-registry
  (:require
   [mount.core :as mount]
   [poker.system.web-pub :as web-pub]
   [poker.game.registry :as registry]))

(mount/defstate registry
  :start
  (registry/make-mem-game-registry web-pub/pub-bus)
  :stop
  (registry/close-mem-game-registry registry))

(comment
  (mount/start))
