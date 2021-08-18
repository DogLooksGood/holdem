(ns poker.system.config
  (:require [cprop.core :as cprop]
            [mount.core :as mount]))

(mount/defstate config
  :start
  (cprop/load-config :resource "config.edn")
  :stop
  nil)
