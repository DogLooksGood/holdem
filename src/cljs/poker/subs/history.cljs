(ns poker.subs.history
  "Game history subscriptions"
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub :history/records
  (fn [db _]
    (let [{:game/keys [id]} db]
      (get-in db [:history/records id]))))
