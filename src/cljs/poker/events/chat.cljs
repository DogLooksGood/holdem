(ns poker.events.chat
  "Chat events"
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx :chat/post-message
  (fn [{:keys [db]} [_ content]]
    (let [{:game/keys [id]} db]
      {:db       db,
       :api/send {:event [:message/new-message
                          {:game-id id,
                           :content content}]}})))

(defn append-message
  [xs x]
  (take 10 (cons x xs)))

(defonce msg-id-serial (atom 1))

;; A message from player, but send by system.
(re-frame/reg-event-db :chat/system-player-request-deal-times
  (fn [db [_ {:keys [player-id deal-times], :as params}]]
    (.log js/console ":chat/system-player-request-deal-times" player-id deal-times)

    (let [game-id (:game/id db)]
      (if (= game-id (:game/id params))
        (update-in db
                   [:chat/messages game-id]
                   append-message
                   {:message/id        (uuid (str (swap! msg-id-serial inc))),
                    :message/sender    player-id,
                    :message/content
                    (if (= deal-times 1)
                      "Run just ONCE!"
                      "Run TWICE?"),
                    :message/timestamp
                    (inst-ms (js/Date.)),
                    :message/game-id
                    game-id})
        db))))

(re-frame/reg-event-fx :chat-server-event/new-message
  (fn [{:keys [db]} [_ message]]
    (let [{:message/keys [game-id]} message]
      (if (= game-id (:game/id db))
        {:db (->> (update-in db
                             [:chat/messages game-id]
                             append-message
                             message)),
         :sound/play-event-sound {:event [:chat-event/new-message]}}
        {:db db}))))
