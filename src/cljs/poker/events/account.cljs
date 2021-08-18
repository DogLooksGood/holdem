(ns poker.events.account
  (:require
   [re-frame.core :as    re-frame
                  :refer [inject-cofx]]
   [poker.events.ws]))

(re-frame/reg-event-fx :account/signup-success
  (fn [{:keys [db]} [_ data]]
    {:db                (merge db data),
     :store             data,
     :ws/connect        {},
     :router/push-state [:lobby]}))

(re-frame/reg-event-db :account/signup-failure
  (fn [db [_ data]]
    (assoc-in db [:errors :signup] (:error data))))

(re-frame/reg-event-fx :account/signup
  (fn [_ [_ data]]
    {:api/post {:api      "/signup",
                :params   data,
                :callback {:success :account/signup-success,
                           :failure :account/signup-failure}}}))

(re-frame/reg-event-fx :account/auth-token
  [(inject-cofx :store)]
  (fn [{:keys [store]} _]
    (cond-> {}
      (:player/token store)
      (assoc :api/post
             {:api      "/auth",
              :params   {:token (:player/token store)},
              :callback {:success :account/signup-success}}))))
