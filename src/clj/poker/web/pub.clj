(ns poker.web.pub
  "Server events publisher.

  Server event format:
  [event-type event-target event-data]
  "
  (:require
   [poker.web.ws          :as ws]
   [clojure.tools.logging :as log]
   [poker.ladder          :as ladder]))

(defonce event-id* (atom 100))

(defmulti handle-server-event
  (fn [event]
    (first event)))

(defmethod handle-server-event :chat-output/new-message
  [[_ message]]
  (let [{:message/keys [to-players]} message]
    (doseq [uid to-players]
      ;; (log/debugf "publish chat message to: %s" uid)
      (ws/send! uid [:chat-server-event/new-message message]))))

(defmethod handle-server-event :ladder/event
  [[_ _state [type data :as evt]]]
  (log/infof "Process ladder event: %s" evt)
  (case type
    :ladder-event/player-returns  (ladder/player-returns data)
    :ladder-event/player-buyin    (ladder/player-buyin data)
    :ladder-event/player-inc-hand (ladder/player-inc-hands data)))

(defmethod handle-server-event :game-output/invalid-event
  [[_ {:keys [props], :as state} [_ event-params :as event]]]
  (when-let [uid (:player-id event-params)]
    (log/infof "Publish player: %s invalid event: %s" (:player/name uid) event)
    (ws/send! uid
              [:game-server-event/invalid-input
               {:game/invalid-event event,
                :game/id            (:game/id props),
                :game/state         state}])))

;; we need remove states those can't be shared
(defmethod handle-server-event :game-output/engine-state
  [[_ state event]]
  (let [{:keys [players props]} state
        player-ids  (keys players)
        share-state (select-keys state
                                 [:status :community-cards :street-bet :min-raise :pots :seats :opts
                                  :runner :showdown :awards :return-bets])]
    (doseq [uid player-ids]
      ;; (log/debugf "publish game state to: %s" uid)
      (if uid
        (let [share-players (->> (for [[id p] players]
                                   [id
                                    (cond-> p
                                      (not= id uid)
                                      (dissoc p :hole-cards))])
                                 (into {}))]
          (ws/send! uid
                    [:game-server-event/game-state-updated
                     {:game/event    event,
                      :game/event-id (swap! event-id* inc),
                      :game/id       (:game/id props),
                      :game/state    (assoc share-state :players share-players)}]))
        (log/errorf "Found null uid, current event: %s, current state: %s"
                    event
                    (prn-str state))))))

(defmethod handle-server-event :lobby-output/updated
  [_]
  (doseq [uid (ws/list-all-uids)]
    (ws/send! uid [:game-server-event/lobby-updated :lobby-updated])))

(defmethod handle-server-event :default
  [event]
  (log/infof "Unsupported server event: %s" (prn-str event)))
