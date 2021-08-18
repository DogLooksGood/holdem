(ns poker.system.web-server
  (:require [aleph.http :as http]
            [mount.core :as mount]
            [poker.web.router :refer [app]]
            [poker.web.ws :as ws]
            [poker.system.config :refer [config]]))

(mount/defstate web-server
  :start
  (http/start-server #'app (get config :web {:port 4000}))
  :stop
  (.close web-server))

(mount/defstate websocket-event-loop
  :start
  (ws/start-event-loop)
  :stop
  (ws/stop-event-loop websocket-event-loop))

(comment
  (mount/stop #'web-server)
  (mount/stop #'websocket-event-loop))
