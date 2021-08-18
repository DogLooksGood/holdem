(ns poker.events.api
  (:require
   [re-frame.core :as re-frame]
   [poker.http    :as http]
   [poker.ws      :as ws]))

(re-frame/reg-fx :api/send
  (fn [{:keys [event callback]}]
    (ws/send! event callback)))

(re-frame/reg-fx :api/post
  (fn [{:keys [api params callback]}]
    (http/post! api params callback)))
