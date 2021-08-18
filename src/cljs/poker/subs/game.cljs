(ns poker.subs.game
  "Game state subscriptions."
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub :player/info
  (fn [db _]
    (select-keys db [:player/id :player/token])))

(re-frame/reg-sub :game/game-id
  (fn [db _]
    (get db :game/id)))

(re-frame/reg-sub :game/state
  (fn [db [_ game-id]]
    (get-in db [:game/states game-id])))

(re-frame/reg-sub :game/local-state
  (fn [db [_ game-id]]
    (get-in db [:game/local-states game-id])))
