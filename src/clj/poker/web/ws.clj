(ns poker.web.ws
  "Websocket setup."
  (:require
   [taoensso.sente        :as sente]
   [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]]
   [clojure.core.async    :as a]
   [clojure.walk          :as walk]
   [clojure.tools.logging :as log]))

(let [{:keys [ch-recv
              send-fn
              connected-uids
              ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defmulti handle-event (fn [_ctx event] (:id event)))

(defmethod handle-event :default
  [_ctx _event])

(defn send!
  "Send data through websocket, convert record to map."
  [uid data]
  (chsk-send! uid
              (walk/postwalk (fn [x] (if (record? x) (into {} x) x)) data)))

(defn list-all-uids
  []
  (get @connected-uids :any))

(defn start-event-loop
  []
  (let [poison (a/chan (a/sliding-buffer 1))]
    (a/go-loop [[evt port] (a/alts! [ch-chsk poison])]
      (when (= port ch-chsk)
        (let [ctx {:send-fn!       chsk-send!,
                   :connected-uids connected-uids}]
          (a/thread (handle-event ctx evt)))
        (recur (a/alts! [ch-chsk poison]))))
    {:poison-ch poison}))

(defn stop-event-loop
  [event-loop]
  (a/>!! (:poison-ch event-loop) :stop))
