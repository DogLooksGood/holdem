(ns poker.events.history
  (:require
   [re-frame.core :as re-frame]))

(defn append-history
  [xs x]
  (take 10 (cons x xs)))

(re-frame/reg-event-db :history/new-record
  (fn [db [_ {:keys [showdown awards street]}]]
    (let [game-id (:game/id db)
          record  (cond
                    (seq showdown)
                    {:showdowns
                     (for [[id {:keys [category picks hole-cards]}]
                           (->> showdown
                                (sort-by (comp awards key)))]
                       {:category   category,
                        :picks      picks,
                        :hole-cards hole-cards,
                        :award      (get awards id),
                        :player-id  id})}

                    (seq awards)
                    {:last-player
                     {:award     (-> awards
                                     first
                                     val),
                      :player-id (-> awards
                                     ffirst),
                      :street    street}})]

      (cond-> db record (update-in [:history/records game-id] append-history record)))))
