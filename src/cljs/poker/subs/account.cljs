(ns poker.subs.account
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub :account/signup-error
  (fn [db _]
    (get-in db [:errors :signup])))
