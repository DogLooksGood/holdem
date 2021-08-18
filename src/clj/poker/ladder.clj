(ns poker.ladder
  "Ladder is the leaderboard for player skills."
  (:require
   [clojure.spec.alpha :as s]
   [poker.specs]
   [poker.system.db    :as db]
   [crux.api           :as crux]))

(s/def ::player-id some?)
(s/def ::borrow int?)
(s/def ::returns int?)

(s/def ::player-buyin-params (s/keys :req-un [::player-id ::buyin]))
(s/def ::player-inc-hands-params (s/keys :req-un [::player-id]))
(s/def ::player-returns-params (s/keys :req-un [::player-id ::returns]))

(defn calc-score
  [player]
  (let [{:ladder/keys [hands buyin returns]} player]
    (if (and (int? hands)
             (int? buyin)
             (int? returns))
      (let [score
            (int
             (* (- returns buyin)
                (/ (+ (* 6 (Math/exp (- (Math/pow (/ hands 50) 2)))) 1))
                (- 1 (/ 0.4 (+ 1 (Math/exp (* 0.00005 (- 120000 buyin))))))))]
        (assoc player :ladder/score score))
      player)))

(defn list-leaderboard
  []
  (let [db (crux/db db/node)]
    (->>
     (crux/q
      db
      '{:find     [(pull
                    p
                    [:player/name :player/avatar :ladder/hands :ladder/buyin :ladder/returns
                     :ladder/score])
                   s],
        :where    [[p :ladder/score s]],
        :order-by [[s :desc]]})
     (mapv first))))

(defn player-inc-hands
  [params]
  {:pre [(s/valid? ::player-inc-hands-params params)]}
  (let [{:keys [player-id]} params
        db         (crux/db db/node)
        player     (crux/entity db player-id)
        new-player (update player
                           :ladder/hands
                           (fnil inc 0))]
    (crux/submit-tx db/node
                    [[:crux.tx/match player-id player]
                     [:crux.tx/put new-player]])))

(defn player-buyin
  [params]
  {:pre [(s/valid? ::player-buyin-params params)]}
  (let [{:keys [player-id buyin]} params
        db         (crux/db db/node)
        player     (crux/entity db player-id)
        new-player (-> player
                       (update
                        :ladder/buyin
                        (fnil + 0)
                        buyin)
                       (calc-score))]
    (crux/submit-tx db/node
                    [[:crux.tx/match player-id player]
                     [:crux.tx/put new-player]])))

(defn player-returns
  [params]
  {:pre [(s/valid? ::player-returns-params params)]}
  (let [{:keys [player-id returns]} params
        db         (crux/db db/node)
        player     (crux/entity db player-id)
        new-player (-> player
                       (update :ladder/returns
                               (fnil + 0)
                               returns)
                       (update :ladder/hands
                               (fnil inc 0))
                       (calc-score))]
    (crux/submit-tx db/node
                    [[:crux.tx/match player-id player]
                     [:crux.tx/put new-player]])))
