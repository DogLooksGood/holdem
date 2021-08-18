(ns poker.ws
  (:require
   [taoensso.sente  :as sente]
   [re-frame.core   :as re-frame]
   [cljs.core.async :as a]
   [poker.events.game]
   [poker.csrf      :refer [?csrf-token]]
   [poker.logger    :as log]))

(def sse-event-types
  #{:game-server-event/game-state-updated
    :game-server-event/invalid-input
    :game-server-event/lobby-updated
    :game-server-event/player-action
    :chat-server-event/new-message})

(def request-timeout 1000)

(defonce socket-conn (a/promise-chan))

(defn handle-common-event
  [e]
  (when (= e [:chsk/ws-ping])
    (re-frame/dispatch [:ws/flush-failed-event])))

(defn connect-socket!
  []
  (a/go
   (when ?csrf-token
     (let [{:keys [chsk ch-recv send-fn state]}
           (sente/make-channel-socket-client! "/chsk"
                                              ?csrf-token
                                              {:type         :auto,
                                               :ws-kalive-ms 5000})]
       ;; Make sure the socket is connected
       (.debug js/console "websocket connected!" (:event (a/<! ch-recv)))
       (a/go-loop [recv (a/<! ch-recv)]
         (when-let [{:keys [event]} recv]
           (let [[_ e] event]
             (if (sse-event-types (first e))
               (do
                 (.debug js/console "receive event" e)
                 (re-frame/dispatch e))
               (handle-common-event e))
             (recur (a/<! ch-recv)))))
       (a/put! socket-conn
               {:chsk       chsk,
                :ch-chsk    ch-recv,
                :chsk-send! send-fn,
                :chsk-state state})))))

(defonce check-loop
  (a/go-loop []
    (a/<! (a/timeout 1000))
    (let [{:keys [chsk-state]} (a/<! socket-conn)]
      (when (and (:ever-opened? @chsk-state)
                 (not (:open? @chsk-state)))
        (re-frame/dispatch [:router/push-state :lobby]))
      (recur))))

(defn make-response-handler
  [callback]
  (fn [reply]
    (.debug js/console "websocket get response " reply)
    (if (and (sente/cb-success? reply) (not (:error reply)))
      (when-let [success (:success callback)]
        (re-frame/dispatch [success reply]))
      (when-let [failure (:failure callback)]
        (re-frame/dispatch [failure reply])))))

;; TODO, refactor
(defn make-default-response-handler
  [event]
  (fn [resp]
    (case resp
      (:chsk/closed :chsk/timeout :chsk/error)
      (re-frame/dispatch [:ws/save-failed-event event])
      (log/info "ws event:" event "\nresponse:" resp))))

(defn send!
  ([event] (send! event nil))
  ([event callback]
   (a/go
    (.debug js/console "websocket send event " event)
    (let [conn (a/<! socket-conn)
          {:keys [chsk-send!]} conn]
      (if callback
        (chsk-send!
         event
         request-timeout
         (make-response-handler callback))
        (chsk-send!
         event
         request-timeout
         (make-default-response-handler event)))))))

(comment
  (send! [:test/test {:a 1}]))
