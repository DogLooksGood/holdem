(ns poker.system.db
  (:require
   [crux.api :as crux]
   [mount.core :as mount]
   [poker.system.config :as config]))

(mount/defstate node
  :start
  (crux/start-node (get-in config/config [:crux :opts]))
  :stop
  ((:close-fn node)))
