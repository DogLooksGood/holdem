(ns poker.utils.test-system
  (:require
   [poker.system.db     :as db]
   [poker.system.game-registry :as game-registry]
   [mount.core          :as mount]
   [crux.api            :as crux]
   [poker.game.registry :as registry]))

(mount/defstate test-node
  :start
  (crux/start-node {})
  :stop
  ((:close-fn test-node)))

(mount/defstate test-registry
  :start
  (registry/make-mem-game-registry nil)
  :stop
  (registry/close-mem-game-registry test-registry))

(defn wrap-test-system
  [run-tests]
  (mount/start #'test-node #'test-registry)
  (with-redefs [db/node test-node
                game-registry/registry test-registry]
    (run-tests))
  (mount/stop #'test-node #'test-registry))
