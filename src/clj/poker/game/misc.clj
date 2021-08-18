(ns poker.game.misc
  "Miscellaneous for engine implementation."
  (:require
   [clojure.set           :as set]
   [poker.utils           :refer [rotate rotate-by map-vals keep-vals]]
   [poker.game.model      :as model]
   [poker.game.evaluator  :as evaluator]
   [poker.specs           :as specs]
   [clojure.tools.logging :as log])
  (:import java.util.UUID))

(def ^:dynamic *cards-shuffle-seed* "The seed used by `create-deck-of-cards.`" nil)

(def players-num->position-list
  "A mapping from index to position keyword.

  BTN - button
  SB  - small blind
  BB  - big blind
  UTG - under the gun
  MP  - middle position
  HJ  - high jack
  CO  - cutoff
  "
  {2 ["BB" "BTN SB"],
   3 ["SB" "BB" "BTN"],
   4 ["SB" "BB" "CO" "BTN"],
   5 ["SB" "BB" "UTG" "CO" "BTN"],
   6 ["SB" "BB" "UTG" "HJ" "CO" "BTN"],
   7 ["SB" "BB" "UTG" "+1" "HJ" "CO" "BTN"],
   8 ["SB" "BB" "UTG" "+1" "+2" "HJ" "CO" "BTN"],
   9 ["SB" "BB" "UTG" "UTG+1" "MP" "MP+1" "HJ" "CO" "BTN"]})

(defn create-deck-of-cards
  "Create a new deck of cards.

  If *cards-shuffle-seed* is an integer, the returned order is deterministic."
  []
  (let [cards (for [suit specs/suits kind specs/kinds] [suit kind])]
    (if *cards-shuffle-seed*
      (let [al  (java.util.ArrayList. cards)
            rng (java.util.Random. *cards-shuffle-seed*)]
        (java.util.Collections/shuffle al rng)
        (clojure.lang.RT/vector (.toArray al)))
      (vec (shuffle cards)))))

(defn init-player-status-for-new-street
  [players action-player-id]
  (->> (for [[id p] players]
         (let [{:keys [status]} p
               rep-status       (cond (= id action-player-id)         :player-status/in-action
                                      (= status :player-status/acted) :player-status/wait-for-action
                                      :else                           status)]
           [id
            (-> p
                (assoc :status rep-status)
                (update :bets conj nil))]))
       (into {})))

(defn find-players-for-start
  [players btn-seat]
  (let [in-game?        (comp #{:player-status/wait-for-start
                                :player-status/join-bet
                                :player-status/wait-for-bb}
                              :status)
        bb?             (comp #{:player-status/wait-for-bb} :status)
        start?          (comp #{:player-status/wait-for-start :player-status/join-bet} :status)
        btn-seat        (or btn-seat 0)
        players-wait-for-game (->> (vals players)
                                   (filter in-game?))
        players-in-order (->> players-wait-for-game
                              (sort-by :seat)
                              (cycle)
                              (take 20)
                              (rotate-by #(and
                                           (start? %)
                                           (< btn-seat (:seat %))))
                              (distinct))
        players-in-order (if (bb? (first players-in-order))
                           (rotate-by start? players-in-order)
                           players-in-order)
        in-game-players (loop [[p & ps] players-in-order
                               rst      []]
                          (cond
                            (nil? p)
                            (rotate 1 rst)

                            (and (= 2 (count rst)) (bb? p))
                            (recur ps (conj rst p))

                            (bb? p)
                            (recur ps rst)

                            :else
                            (recur ps (conj rst p))))]
    (if (> 2 (count in-game-players))
      players-in-order
      in-game-players)))


(defn collapse-pots
  [street pots]
  (reduce (fn [acc pot]
            (if (and (= (:player-ids (peek acc)) (:player-ids pot))
                     ;; Current street pot can only collapsed with pots in same street.
                     (= (= street (:street (peek acc)))
                        (= street (:street pot))))
              (conj (pop acc)
                    (update pot
                            :value
                            +
                            (-> acc
                                peek
                                :value)))
              (conj acc pot)))
          [(first pots)]
          (next pots)))

(def game-status->street
  {:game-status/preflop :preflop,
   :game-status/flop    :flop,
   :game-status/turn    :turn,
   :game-status/river   :river})

(def bet-idx->street
  [:preflop :flop :turn :river])

(defn count-pot
  "Count the pot for players.

  The pots will be collapsed by their owners, except those pot on current street."
  [curr-street players]
  (let [id->bets (->> (for [[id p] players]
                        (if (#{:player-status/acted
                               :player-status/all-in
                               :player-status/in-action
                               :player-status/wait-for-action}
                             (:status p))
                          [id (:bets p)]
                          [[id :fold] (:bets p)]))
                      (into {}))
        n        (->> id->bets
                      (vals)
                      (map count)
                      (reduce max 0))]
    (loop [pots []
           idx  0]
      (let [bet->ids (->> id->bets
                          (keep-vals #(get % idx))
                          (group-by val)
                          (map-vals (comp set keys))
                          (sort-by first))]
        (if (= idx n)
          ;; return the whole pots
          ;; collapse the unneccessary split
          (collapse-pots curr-street pots)
          ;; split pots
          (let [all-ids         (set (mapcat val bet->ids))
                [_ _ more-pots] (reduce
                                 (fn [[prev-bet ex-ids ps] [bet ids]]
                                   (let [bet-ids   (set/difference all-ids ex-ids)
                                         total-bet (* (- bet prev-bet) (count bet-ids))]
                                     [bet
                                      (set/union ex-ids ids)
                                      (conj ps
                                            (model/make-pot
                                             {:player-ids (into #{} (remove vector?) bet-ids),
                                              :street     (bet-idx->street idx),
                                              :value      total-bet}))]))
                                 [0 #{} []]
                                 bet->ids)]
            (recur (into pots more-pots) (inc idx))))))))

(defn calc-showdown
  "Calculate showdown result."
  [{:keys [players pots community-cards in-game-player-ids]}]
  (let [showdown (->> players
                      (vals)
                      (filter (comp #{:player-status/acted :player-status/all-in} :status))
                      (map (fn [{:keys [id hole-cards]}]
                             [id
                              (assoc (evaluator/evaluate-cards (concat hole-cards community-cards))
                                     :hole-cards
                                     hole-cards)]))
                      (into {}))
        rep-pots (map (fn [{:keys [player-ids], :as pot}]
                        (let [winner-ids (some->> player-ids
                                                  (map (juxt identity showdown))
                                                  (filter second)
                                                  ;; [id, {category, value, picks}]
                                                  (group-by (comp :value second))
                                                  (sort-by first)
                                                  (last)
                                                  (val)
                                                  ;; [[id, {}], [id, {}]]
                                                  (map first))]
                          (assoc pot :winner-ids winner-ids)))
                      pots)
        awards   (->> rep-pots
                      (filter :winner-ids)
                      (mapcat
                       (fn [{:keys [value winner-ids]}]
                         (let [cnt       (count winner-ids)
                               undivided (mod value cnt)
                               award     (quot value cnt)
                               first-player-id (some (set winner-ids) in-game-player-ids)]
                           (cond-> (map vector winner-ids (repeat award))
                             (pos? undivided)
                             (conj [first-player-id undivided])))))
                      (group-by first)
                      (map-vals #(reduce + (map second %))))]
    {:pots rep-pots, :awards awards, :showdown showdown}))

(defn reward-to-players
  [awards players]
  {:pre [(map? awards) (map? players)]}
  (map-vals (fn [p]
              (if-let [award (get awards (:id p))]
                (update p :stack + award)
                p))
            players))

(defn set-in-action-for-player
  [state player-id]
  (let [is-dropout? (= :network-error/dropout
                       (get-in state
                               [:players
                                player-id
                                :network-error]))]
    (if is-dropout?
      (update state
              :next-events
              conj
              [:game-event/player-fold
               {:player-id player-id}])
      (let [action-id (UUID/randomUUID)]
        (-> state
            (update
             :schedule-events
             conj
             (model/make-schedule-event
              {:timeout (* (:action-secs (:opts state)) 1000),
               :event   [:game-event/fold-player
                         {:player-id
                          player-id,
                          :action-id
                          action-id}]}))
            (assoc :action-id action-id))))))
