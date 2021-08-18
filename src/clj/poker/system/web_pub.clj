(ns poker.system.web-pub
  ;; We use a channel for pub bus
  (:require
   [poker.web.pub         :as pub]
   [mount.core            :as mount]
   [clojure.core.async    :as a]
   [clojure.tools.logging :as log])
  (:import clojure.lang.ExceptionInfo))

;; single thread version
(defn start-pub-bus
  []
  (let [ch (a/chan 256)]
    (a/go-loop [event (a/<! ch)]
      (when event
        (try
          (pub/handle-server-event event)
          (catch ExceptionInfo ex
            (log/errorf ex "handle server event error(I), %s %s" (ex-message ex) (ex-data ex)))
          (catch Exception ex
            (log/error ex "handle server event error(II)")))
        (recur (a/<! ch))))
    ch))

(mount/defstate pub-bus
  :start
  (start-pub-bus)
  :stop
  (a/close! pub-bus))
