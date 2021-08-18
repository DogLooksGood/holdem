(ns poker.events.ladder
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db :ladder/list-leaderboard-success
  (fn [db [_ players]]
    (assoc db :ladder/leaderboard players)))

(re-frame/reg-event-fx :ladder/list-leaderboard
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event    [:ladder/list-leaderboard {}],
                :callback {:success :ladder/list-leaderboard-success}}}))
