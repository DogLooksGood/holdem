(ns poker.subs.chat
  "Chat subscriptions"
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub :chat/messages
  (fn [db _]
    (let [{:game/keys [id]} db]
      (get-in db [:chat/messages id]))))
