(ns poker.events.ws
  (:require
   [re-frame.core :as re-frame]
   [poker.logger  :as log]
   [poker.ws      :as ws]))

(re-frame/reg-fx :ws/connect
  (fn [_]
    (ws/connect-socket!)))

(re-frame/reg-event-db :ws/save-failed-event
  (fn [db [_ evt]]
    (log/info "save failed event: " evt)
    (assoc db :ws/failed-event evt)))

(re-frame/reg-event-fx :ws/flush-failed-event
  (fn [{:keys [db]} _]
    (if-let [e (:ws/failed-event db)]
      (do
        (log/info "flush failed event" e)
        {:db       (dissoc db :ws/failed-event),
         :api/send {:event e}})
      {:db db})))
