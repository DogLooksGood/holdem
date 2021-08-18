(ns poker.subs.ladder
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub :ladder/leaderboard
  (fn [db _]
    (get db :ladder/leaderboard)))
