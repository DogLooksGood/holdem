(ns poker.events.lobby
  (:require
   [poker.logger  :as log]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx :game-server-event/lobby-updated
  (fn [_ _]

    {:dispatch-n [[:lobby/list-games]
                  [:lobby/list-players]]}))

(re-frame/reg-event-db :lobby/list-games-success
  (fn [db [_ games]]
    (assoc db :lobby/games games)))

(re-frame/reg-event-db :lobby/list-players-success
  (fn [db [_ players]]
    (assoc db :lobby/online-player-ids players)))

(re-frame/reg-event-fx :lobby/list-games
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event    [:lobby/list-games {}],
                :callback {:success :lobby/list-games-success}}}))

(re-frame/reg-event-fx :lobby/list-players
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event    [:lobby/list-players {}],
                :callback {:success :lobby/list-players-success}}}))

(re-frame/reg-event-fx :lobby/create-game
  (fn [{:keys [db]} [_ {:game/keys [title]}]]
    (let [{:player/keys [token]} db]
      {:db       db,
       :api/send {:event    [:lobby/create-game
                             {:player/token token,
                              :game/title   title}],
                  :callback {:success :lobby/join-game-success}}})))

(re-frame/reg-event-fx :lobby/join-game-success
  (fn [{:keys [db]} [_ {:game/keys [id]}]]
    {:db (assoc db :game/id id),
     :router/push-state [:game {:game-id id}]}))

(re-frame/reg-event-fx :lobby/join-game
  (fn [{:keys [db]} [_ {:game/keys [id]}]]
    (let [{:player/keys [token]} db]
      {:db       db,
       :dispatch [:game/flush-local-state {:game/id id}],
       :api/send {:event    [:lobby/join-game
                             {:player/token token,
                              :game/id      id}],
                  :callback {:success :lobby/join-game-success}}})))

(re-frame/reg-event-fx :lobby/back-to-game
  (fn [{:keys [db]} [_ {:game/keys [id]}]]
    {:db (assoc db :game/id id),
     :router/push-state [:game {:game-id id}]}))

(re-frame/reg-event-fx :lobby/leave-game-success
  (fn [{:keys [db]} [_ _]]
    {:db (dissoc db :game/id),
     :router/push-state [:lobby]}))

(re-frame/reg-event-fx :lobby/leave-game
  (fn [{:keys [db]} _]
    (log/info "lobby/leave-game:" db)
    (let [{:game/keys [id], :player/keys [token]} db]
      {:db       db,
       :api/send {:event    [:lobby/leave-game
                             {:player/token token,
                              :game/id      id}],
                  :callback {:success :lobby/leave-game-success}}})))
