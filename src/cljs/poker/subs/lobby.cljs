(ns poker.subs.lobby
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub :lobby/games
  (fn [db _]
    (get db :lobby/games)))

(re-frame/reg-sub :lobby/online-player-ids
  (fn [db _]
    (get db :lobby/online-player-ids)))
