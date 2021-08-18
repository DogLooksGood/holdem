(ns poker.game.event-handler
  "Implementations of engine's event handler."
  (:require
   [poker.game.misc       :as misc]
   [poker.game.model      :as model]
   [poker.game.event-handler.impl :as impl]
   [poker.utils           :refer [filter-vals map-vals]]
   [clojure.tools.logging :as log]))

;; errors

(defn throw-no-enough-stack!
  [event]
  (throw (ex-info "No enough stack" {:event event})))

(defn throw-invalid-player-id!
  [event]
  (throw (ex-info "Invalid player id" {:event event})))

(defn throw-player-already-buyin!
  [event]
  (throw (ex-info "Player already buyin" {:event event})))

(defn throw-great-than-max-buyin!
  [event]
  (throw (ex-info "Great than max buyin" {:event event})))

(defn throw-player-not-wait-for-bb!
  [event]
  (throw (ex-info "Player not wait for bb" {:event event})))
(defn throw-expired-event! [event] (throw (ex-info "Expired event" {:event event})))
(defn throw-game-already-started! [event] (throw (ex-info "Game already started" {:event event})))
(defn throw-less-than-min-buyin! [event] (throw (ex-info "Less than min buyin" {:event event})))
(defn throw-seat-is-occupied! [event] (throw (ex-info "Seat is occupied" {:event event})))
(defn throw-player-not-in-action! [event] (throw (ex-info "Player not in action" {:event event})))
(defn throw-no-enough-players! [event] (throw (ex-info "No enough players" {:event event})))
(defn throw-cant-bet! [event] (throw (ex-info "Can't bet" {:event event})))
(defn throw-bet-is-too-small! [event] (throw (ex-info "Bet is too small" {:event event})))
(defn throw-raise-is-too-small! [event] (throw (ex-info "Raise is too small" {:event event})))
(defn throw-cant-call! [event] (throw (ex-info "Can't call" {:event event})))
(defn throw-cant-check! [event] (throw (ex-info "Can't check" {:event event})))
(defn throw-cant-raise! [event] (throw (ex-info "Can't raise" {:event event})))
(defn throw-invalid-times! [event] (throw (ex-info "Invalid times" {:event event})))
(defn throw-invalid-event! [event] (throw (ex-info "Invalid event" {:event event})))
(defn throw-cant-musk! [event] (throw (ex-info "Can't musk" {:event event})))
(defn throw-last-player-cant-musk!
  [event]
  (throw (ex-info "Last player can't musk" {:event event})))
(defn throw-player-already-in-game!
  [event]
  (throw (ex-info "Player already in game" {:event event})))
(defn throw-duplicate-deal-times-request
  [event]
  (throw (ex-info "Duplicate deal times request" {:event event})))

;; assertions

(defn assert-enough-players!
  [state event]
  (when (->> state
             (:players)
             (vals)
             (filter (comp #{:player-status/wait-for-start :player-status/wait-for-bb
                             :player-status/join-bet}
                           :status))
             (count)
             (> 2))
    (throw-no-enough-players! event)))

(defn assert-player-not-in-game!
  [state event id]
  (when-let [p (get-in state [:players id])]
    (when-not (= :player-status/leave (:status p))
      (throw-player-already-in-game! event))))

(defn assert-player-id!
  [state event id]
  (when-not (get-in state [:players id]) (throw-invalid-player-id! event)))

(defn assert-player-off-seat!
  [state event id]
  (when-not (= :player-status/off-seat (get-in state [:players id :status]))
    (throw-player-already-buyin! event)))

(defn assert-buyin-stack!
  [state event stack]
  (let [{:keys [min-buyin max-buyin]} (get state :opts)]
    (cond (< max-buyin stack) (throw-great-than-max-buyin! event)
          (> min-buyin stack) (throw-less-than-min-buyin! event))))

(defn assert-seat-is-available!
  [state event seat]
  (when (get-in state [:seats seat])
    (throw-seat-is-occupied! event)))

(defn assert-player-can-call!
  [state event player-id]
  (let [{:keys [street-bet]} state
        current-bet (or (peek (get-in state [:players player-id :bets])) 0)]
    (when-not (pos? street-bet)
      (throw-cant-call! event))
    (when-not (> street-bet current-bet)
      (throw-cant-call! event))))

(defn assert-game-can-start!
  [state event]
  (when-not (= :game-status/idle (:status state))
    (throw-game-already-started! event)))

(defn assert-action-player-id!
  [state event player-id]
  (when-not (= :player-status/in-action (get-in state [:players player-id :status]))
    (throw-player-not-in-action! event)))

(defn assert-valid-action-id!
  [state event action-id]
  (when-not (= action-id (:action-id state))
    (throw-expired-event! event)))

(defn assert-valid-bet!
  [state event player-id bet]
  (cond (< bet (get-in state [:opts :bb])) (throw-bet-is-too-small! event)
        (> bet (get-in state [:players player-id :stack])) (throw-no-enough-stack! event)))

(defn assert-valid-raise!
  [state event player-id raise]
  (let [player       (get-in state [:players player-id])
        {:keys [bets stack]} player
        curr-bet     (last bets)
        {:keys [street-bet opts min-raise]}
        state
        {:keys [bb]}
        opts]
    (cond
      (and (< (+ (or curr-bet 0) raise)
              (+ (or street-bet 0) (or min-raise street-bet bb 0)))
           (not= stack raise))
      (throw-raise-is-too-small! event)

      (> raise (get-in state [:players player-id :stack]))
      (throw-no-enough-stack! event)
      ;; Only acted and wait-for-action can call the raise
      (->> (:players state)
           (keep (fn [[id p]]
                   (when-not (= player-id id)
                     (#{:player-status/acted :player-status/wait-for-action} (:status p)))))
           (empty?))
      (throw-cant-raise! event))))

(defn assert-valid-check!
  [state event player-id]
  (when-not (= (peek (get-in state [:players player-id :bets])) (get state :street-bet))
    (throw-cant-check! event)))

(defn assert-valid-deal-times-request!
  [state event player-id times]
  (when-not (int? times) (throw-invalid-times! event))
  (when-not (#{:player-status/acted :player-status/all-in}
             (get-in state [:players player-id :status]))
    (throw-player-not-in-action! event))
  (let [curr-times (get-in state [:runner :deal-times] 2)]
    (when-not (<= 1 times curr-times) (throw-invalid-times! event)))
  (when (get-in state [:runner :player-deal-times player-id])
    (throw-duplicate-deal-times-request event)))

(defn assert-can-bet!
  [state event]
  (when (:street-bet state) (throw-cant-bet! event)))

(defn assert-player-wait-for-bb!
  [state event player-id]
  (when-not (= :player-status/wait-for-bb (get-in state [:players player-id :status]))
    (throw-player-not-wait-for-bb! event)))

(defn assert-player-can-musk!
  [state event player-id]
  (when-not (->> (:players state)
                 (vals)
                 (some (fn [{:keys [status id]}]
                         (and (not= id player-id)
                              (#{:player-status/all-in :player-status/acted} status)))))
    (throw-last-player-cant-musk! event))
  (when-not (= :player-status/acted (get-in state [:players player-id :status]))
    (throw-cant-musk! event))
  (when-not (= :game-status/showdown-prepare (:status state)) (throw-cant-musk! event)))

(defn assert-can-request-deal-times!
  [state event]
  (when-not (= (:status state) :game-status/runner-prepare) (throw-invalid-event! event)))

(defn dispatch-by-event-type [_game-state game-event] (first game-event))

(defmulti validate-game-event
  "Validate game event."
  dispatch-by-event-type)

(defmethod validate-game-event :default
  [_ _])

(defmulti apply-game-event
  "Apply game event to game state."
  dispatch-by-event-type)

(defmethod apply-game-event :default
  [_ event]
  (throw-invalid-event! event))

(defn handle-game-event
  "Game event handler dispatch by event-type."
  [game-state game-event]
  (validate-game-event game-state game-event)
  (apply-game-event game-state game-event))

;; ----------------------------------------------------------------------------
;; validate game event
;; ----------------------------------------------------------------------------

;; Ensure the player is not in the game
(defmethod validate-game-event :game-event/player-join
  [game-state
   [_ {:keys [id]}
    :as
    event]]
  (assert-player-not-in-game! game-state event id))

(defmethod validate-game-event :game-event/player-leave
  [game-state [_ {:keys [player-id] :as event}]]
  (assert-player-id! game-state event player-id))

;; Check player id
;; Ensure player status is off-seat
;; Ensure stack-add is between max-buyin & min-buyin
;; Ensure the seat is available
(defmethod validate-game-event :game-event/player-buyin
  [game-state
   [_ {:keys [player-id seat stack-add]}
    :as
    event]]
  (assert-player-id! game-state event player-id)
  (assert-player-off-seat! game-state event player-id)
  (assert-buyin-stack! game-state event stack-add)
  (assert-seat-is-available! game-state event seat))

;; Ensure the game is idle.
(defmethod validate-game-event :game-event/start-game
  [game-state event]
  ;; Here we need two players to start a game
  (assert-game-can-start! game-state event)
  (assert-enough-players! game-state event))

(defmethod validate-game-event :game-event/player-join-bet
  [game-state
   [_ {:keys [player-id]}
    :as
    event]]
  (assert-player-id! game-state event player-id)
  (assert-player-wait-for-bb! game-state event player-id))

(defmethod validate-game-event :game-event/player-call
  [game-state
   [_ {:keys [player-id]}
    :as
    event]]
  (assert-action-player-id! game-state event player-id)
  (assert-player-can-call! game-state event player-id))

(defmethod validate-game-event :game-event/player-bet
  [game-state
   [_ {:keys [player-id bet]}
    :as
    event]]
  (assert-action-player-id! game-state event player-id)
  (assert-can-bet! game-state event)
  (assert-valid-bet! game-state event player-id bet))

(defmethod validate-game-event :game-event/player-raise
  [game-state
   [_ {:keys [player-id raise]}
    :as
    event]]
  (assert-action-player-id! game-state event player-id)
  (assert-valid-raise! game-state event player-id raise))

(defmethod validate-game-event :game-event/player-fold
  [game-state
   [_ {:keys [player-id]}
    :as
    event]]
  (assert-action-player-id! game-state event player-id))

(defmethod validate-game-event :game-event/fold-player
  [game-state
   [_ {:keys [player-id action-id], :as event}]]
  (assert-action-player-id! game-state event player-id)
  (assert-valid-action-id! game-state event action-id))

(defmethod validate-game-event :game-event/player-check
  [game-state
   [_ {:keys [player-id]}
    :as
    event]]
  (assert-action-player-id! game-state event player-id)
  (assert-valid-check! game-state event player-id))

(defmethod validate-game-event :game-event/player-musk
  [game-state [_ {:keys [player-id], :as event}]]
  (assert-player-id! game-state event player-id)
  (assert-player-can-musk! game-state event player-id))

(defmethod validate-game-event :game-event/player-request-deal-times
  [game-state
   [_ {:keys [player-id deal-times]}
    :as
    event]]
  (assert-player-id! game-state event player-id)
  (assert-can-request-deal-times! game-state event)
  (assert-valid-deal-times-request! game-state event player-id deal-times))

(defmethod validate-game-event :game-event/player-get-current-state
  [game-state [_ {:keys [player-id], :as event}]]
  (assert-player-id! game-state event player-id))

;; ----------------------------------------------------------------------------
;; apply game event
;; ----------------------------------------------------------------------------

;; Add player to game with initial state.
(defmethod apply-game-event :game-event/player-join
  [game-state
   [_ {:keys [id name props]}]]
  (let [player (model/make-player-state {:id id, :name name, :props props})]
    (impl/add-player game-state player)))

;; Kick player immediately if current game state is idle or settlement.
;; Mark player as dropout, if still in game.
;; The dropout player can reconnect in this game, or be kicked out before start next game.
(defmethod apply-game-event :game-event/player-leave
  [game-state [_ {:keys [player-id]}]]
  (let [{:keys [players status]} game-state]
    (if
      (or (#{:game-status/showdown-settlement
             :game-status/runner-settlement
             :game-status/last-player-settlement
             :game-status/idle}
           status)
          (#{:player-status/wait-for-bb
             :player-status/join-bet
             :player-status/off-seat}
           (get-in players [player-id :status])))
      ;; kick player
      (impl/remove-player game-state player-id)
      ;; mark player's network is dropout
      (impl/mark-player-leave game-state player-id))))

;; Buyin will trigger a game-start immediately.
(defmethod apply-game-event :game-event/player-buyin
  [game-state
   [_ {:keys [player-id seat stack-add no-auto-start?]}]]
  (let [next-events     (:next-events game-state)
        rep-next-events (if no-auto-start?
                          next-events
                          (conj next-events
                                [:game-event/start-game {}]))]
    (-> game-state
        (impl/player-set-stack player-id stack-add)
        (impl/player-set-seat player-id seat)
        (update :ladder-events
                conj
                [:ladder-event/player-buyin {:player-id player-id, :buyin stack-add}])
        (assoc :next-events rep-next-events))))

;; Prepare state for next-game:
;; In this procedure we reset nearly all game states.
;; we kick dropout players
;; we update btn-seat to next player
;; Players without stack will be set as off-seat.
(defmethod apply-game-event :game-event/next-game
  [game-state _]
  (let [state (loop [state game-state
                     [{:keys [id status stack network-error], :as p} & ps] (vals (:players
                                                                                  game-state))]
                (cond
                  (nil? p)
                  state

                  (some? network-error)
                  (recur (impl/remove-player state id) ps)

                  (= :player-status/leave status)
                  (recur (impl/remove-player state id) ps)

                  (and (int? stack) (zero? stack))
                  (recur (impl/set-player-off-seat state id) ps)

                  (#{:player-status/wait-for-bb
                     :player-status/join-bet
                     :player-status/off-seat}
                   status)
                  (recur state ps)

                  :else
                  (recur (impl/reset-player-state-for-new-game state id) ps)))]
    (-> state
        (assoc
         :status             :game-status/idle
         :in-game-player-ids []
         :cards              nil
         :community-cards    []
         :pots               nil
         :street-bet         nil
         :min-raise          nil
         :showdown           nil
         :awards             nil
         :return-bets        nil
         :runner             nil
         :player-action      nil)
        (update :schedule-events
                conj
                (model/make-schedule-event {:timeout 1000,
                                            :event   [:game-event/check-chip-leader {}]})
                (model/make-schedule-event {:timeout 2000,
                                            :event   [:game-event/start-game {}]})))))

;; GAME START
;; The following setups will be done during start-game
;; 1. collect the players in this game
;; 2. create a deck of cards
;; 3. deliver cards for players.
;; 4. blind bet
;; 5. calculate round bet & pot
;; 6. next player to call
;; This event handler will check if it's possible to start
(defmethod apply-game-event :game-event/start-game
  [game-state _event]
  (-> game-state
      ;; some states should be reset
      (assoc :status :game-status/preflop)
      (assoc :next-events
             [[:game-event/collect-players {}] [:game-event/deal-cards {}]])))

;; collect the player who is the next of the seat in game
;; when finished, goto next street.
(defmethod apply-game-event :game-event/collect-players
  [game-state _]
  (let [{:keys [opts players btn-seat]} game-state
        {:keys [bb sb]} opts]
    (letfn [(make-player-status-map
             [id pos]
             [id {:position pos, :status :player-status/wait-for-action, :bets [nil]}])]
      (let [in-game-players    (misc/find-players-for-start players btn-seat)
            in-game-player-ids (mapv :id in-game-players)
            pos-list           (misc/players-num->position-list (count in-game-player-ids))
            player-status-map  (->> in-game-player-ids
                                    (map-indexed #(make-player-status-map %2 (nth pos-list %1)))
                                    (into {}))
            two-player?        (= 2 (count in-game-player-ids))
            bb-events          (if two-player?
                                 [[:game-event/blind-bet
                                   {:player-id (nth in-game-player-ids
                                                    1),
                                    :bet       sb}]
                                  [:game-event/blind-bet
                                   {:player-id (nth in-game-player-ids
                                                    0),
                                    :bet       bb}]]
                                 (->> [sb bb]
                                      (map-indexed (fn [idx bet]
                                                     (let [id (nth in-game-player-ids idx)]
                                                       (when (not= :player-status/join-bet
                                                                   (get-in players [id :status]))
                                                         [:game-event/blind-bet
                                                          {:player-id id,
                                                           :bet       bet}]))))
                                      (filter some?)
                                      (vec)))
            join-bet-events    (if two-player?
                                 []
                                 (->> in-game-players
                                      (filter (comp #{:player-status/join-bet} :status))
                                      (map (fn [{:keys [id]}]
                                             [:game-event/blind-bet
                                              {:player-id id,
                                               :bet       bb}]))
                                      (vec)))
            rest-events        (if two-player?
                                 ;; start from BTN
                                 [[:game-event/count-pot {}]
                                  [:game-event/next-player-or-stage
                                   {:player-id (nth in-game-player-ids
                                                    0)}]]
                                 ;; start from UTG
                                 [[:game-event/count-pot {}]
                                  [:game-event/next-player-or-stage
                                   {:player-id (nth in-game-player-ids
                                                    1)}]])
            next-events        (vec (concat join-bet-events bb-events rest-events))]
        (log/infof "next-events: %s" next-events)
        (-> game-state
            (assoc :in-game-player-ids in-game-player-ids)
            (assoc :btn-seat
                   (-> in-game-players
                       last
                       :seat))
            (update :players (partial merge-with merge) player-status-map)
            (update :next-events into next-events))))))

(defmethod apply-game-event :game-event/deal-cards
  [game-state _]
  (let [deck-of-cards         (misc/create-deck-of-cards)
        {:keys [in-game-player-ids]} game-state
        player-cnt            (count in-game-player-ids)
        hole-cards-list       (->> deck-of-cards
                                   (partition 2)
                                   (take player-cnt))
        rest-cards            (drop (* 2 player-cnt) deck-of-cards)
        player-hole-cards-map (->> (map (fn [id hole-cards] [id {:hole-cards hole-cards}])
                                        in-game-player-ids
                                        hole-cards-list)
                                   (into {}))]
    (-> game-state
        (assoc :cards rest-cards)
        (update :players (partial merge-with merge) player-hole-cards-map))))

(defmethod apply-game-event :game-event/chip-leader-returns
  [game-state [_ {:keys [player-id returns]}]]
  (impl/player-returns game-state player-id returns))

(defmethod apply-game-event :game-event/check-chip-leader
  [game-state _]
  (let [{:keys [players opts]} game-state
        {:keys [max-stack]} opts
        events (->> players
                    (vals)
                    (keep (fn [{:keys [stack id]}]
                            (when (and (int? stack)
                                       (> stack max-stack))
                              [:game-event/chip-leader-returns
                               {:player-id id,
                                :returns   (- stack max-stack)}]))))]
    (if (seq events)
      (update game-state :next-events into events)
      game-state)))

(defmethod apply-game-event :game-event/count-pot
  [game-state [_ {:keys [collapse-all?]}]]
  (let [{:keys [status players]} game-state
        street     (misc/game-status->street status)
        rep-pots   (misc/count-pot (when-not collapse-all? street) players)
        street-bet (->> players
                        (vals)
                        (keep (comp peek :bets))
                        (reduce max 0))
        ;; street-bet can't be zero
        street-bet (when-not (zero? street-bet) street-bet)]
    (-> game-state
        (assoc :street-bet street-bet :pots rep-pots))))

;; NEXT PLAYER OR STREET

(defmethod apply-game-event :game-event/next-player-or-stage
  [game-state [_ {:keys [player-id]}]]
  (let [{:keys [in-game-player-ids players street-bet opts status]} game-state
        ;; Here we try to found the `next-player-id` to act
        ;; he either has wait-for-action status or bet is less than `street-bet`.
        ;; we have to check according to the order in `in-game-player-ids`.
        [before after]        (split-with #(not= player-id %) in-game-player-ids)
        check-ids             (concat (next after) before)
        rest-players          (->> check-ids
                                   (map players))
        next-act-player-id    (some->> rest-players
                                       (filter #(or
                                                 ;; no action yet
                                                 (= :player-status/wait-for-action (:status %))
                                                 ;; acted, but other player raise
                                                 (and (not= street-bet (peek (:bets %)))
                                                      (= :player-status/acted (:status %)))))
                                       (first)
                                       (:id))
        ;; Here we filter for the rest players(players who not fold).
        left-players          (->> players
                                   (vals)
                                   (filter (comp #{:player-status/wait-for-action
                                                   :player-status/acted :player-status/all-in}
                                                 :status)))
        allin-player-count    (->> left-players
                                   (filter (comp #{:player-status/all-in} :status))
                                   (count))
        ;; first act player for next street
        next-street-player-id (->> in-game-player-ids
                                   (map players)
                                   (filter #(= :player-status/acted (:status %)))
                                   (first)
                                   (:id))
        state-next-street     (fn [state evt]
                                (-> state
                                    (update :next-events
                                            conj
                                            [evt {:action-player-id next-street-player-id}]
                                            [:game-event/count-pot {}])
                                    (misc/set-in-action-for-player next-street-player-id)))]
    (cond
      ;; One player left
      (= 1 (count left-players))      (-> game-state
                                          (update :next-events
                                                  conj
                                                  [:game-event/last-player
                                                   {:player-id (:id (first left-players))}]))
      ;; Wait action from next player
      next-act-player-id
      (let [rep-players (->> (for [[id p] players]
                               [id
                                (cond-> p
                                  (= id next-act-player-id)
                                  (assoc :status :player-status/in-action))])
                             (into {}))]
        (-> game-state
            (assoc :players rep-players)
            (misc/set-in-action-for-player next-act-player-id)))
      ;; Showdown
      (and (nil? next-act-player-id) (= :game-status/river status)) (update
                                                                     game-state
                                                                     :next-events
                                                                     conj
                                                                     [:game-event/count-pot
                                                                      {:collapse-all? true}]
                                                                     [:game-event/showdown-prepare
                                                                      {}])
      ;; Less equal than one player is not allin
      ;; -> runner
      (>= allin-player-count (dec (count left-players)))
      (-> game-state
          (update :next-events
                  conj
                  [:game-event/count-pot {:collapse-all? true}]
                  [:game-event/runner-prepare {}]))
      ;; Preflop -> Flop
      (= :game-status/preflop status) (state-next-street game-state :game-event/flop-street)
      ;; Flop -> Turn
      (= :game-status/flop status)    (state-next-street game-state :game-event/turn-street)
      ;; Turn -> River
      (= :game-status/turn status)    (state-next-street game-state :game-event/river-street)

      :else
      ;; There's no player in game.
      ;; For all player has left.
      ;; Here we call next-game to clean states.
      (update game-state :next-events conj [:game-event/next-game {}]))))

(defmethod apply-game-event :game-event/flop-street
  [game-state [_ {:keys [action-player-id]}]]
  (let [{:keys [cards players]} game-state
        [community-cards rest-cards] (split-at 3 cards)
        rep-players (misc/init-player-status-for-new-street players action-player-id)]
    (-> game-state
        (assoc :street-bet      nil
               :min-raise       nil
               :cards           rest-cards
               :community-cards (vec community-cards)
               :status          :game-status/flop
               :players         rep-players)
        (assoc-in [:players action-player-id :status] :player-status/in-action))))

(defmethod apply-game-event :game-event/turn-street
  [game-state [_ {:keys [action-player-id]}]]
  (let [{:keys [cards players]} game-state
        rep-players (misc/init-player-status-for-new-street players action-player-id)]
    (-> game-state
        (assoc :street-bet nil :min-raise nil :status :game-status/turn :players rep-players)
        (update :community-cards conj (first cards))
        (update :cards next)
        (assoc-in [:players action-player-id :status] :player-status/in-action))))

(defmethod apply-game-event :game-event/river-street
  [game-state [_ {:keys [action-player-id]}]]
  (let [{:keys [cards players]} game-state
        rep-players (misc/init-player-status-for-new-street players action-player-id)]
    (-> game-state
        (assoc :street-bet nil :min-raise nil :players rep-players :status :game-status/river)
        (update :community-cards conj (first cards))
        (update :cards next)
        (assoc-in [:players action-player-id :status] :player-status/in-action))))


;; Newcomer's bet
;; mark player will bet at next game.
(defmethod apply-game-event :game-event/player-join-bet
  [game-state
   [_ {:keys [player-id]}]]
  (impl/mark-player-join-bet game-state player-id))

;; PLAYER ACTIONS
;; each player as 4 types of actions
;; 1. call
;; 2. fold
;; 3. raise
;; 4. check

;; CALL
(defmethod apply-game-event :game-event/player-call
  [game-state
   [_ {:keys [player-id]}]]
  (let [{:keys [street-bet players]} game-state
        bets        (get-in players [player-id :bets])
        current-bet (or (peek bets) 0)
        stack       (get-in players [player-id :stack])
        stack-sub   (- street-bet current-bet)
        [status bet stack] (if (>= stack-sub stack)
                             [:player-status/all-in (+ current-bet stack) 0]
                             [:player-status/acted street-bet (- stack stack-sub)])
        rep-bets    (conj (pop bets) bet)]
    (-> game-state
        (update-in [:players player-id] assoc :status status :bets rep-bets :stack stack)
        (update :next-events
                conj
                [:game-event/count-pot {}]
                [:game-event/next-player-or-stage {:player-id player-id}])
        ;; (assoc :min-raise bet)
        (assoc :player-action [:player-action/call {:player-id player-id}]))))

;; BET
;; the bet must great than BB
;; the bet must less equal than stack
;; if bet equals stack, means all-in
(defmethod apply-game-event :game-event/blind-bet
  [game-state
   [_ {:keys [player-id bet]}]]
  (let [{:keys [players opts]} game-state
        {:keys [stack bets]} (get players player-id)
        status (if (= bet stack) :player-status/all-in :player-status/wait-for-action)
        rep-bets (conj (pop bets) bet)]
    (-> game-state
        (update-in [:players player-id] assoc :bets rep-bets :status status :stack (- stack bet))
        (assoc :street-bet (:bb opts))
        (assoc :min-raise (:bb opts)))))

(defmethod apply-game-event :game-event/player-bet
  [game-state
   [_ {:keys [player-id bet]}]]
  (let [{:keys [players]} game-state
        {:keys [stack bets]} (get players player-id)
        all-in?           (= bet stack)
        status            (if all-in? :player-status/all-in :player-status/acted)
        rep-bets          (conj (pop bets) bet)]
    (-> game-state
        (update-in [:players player-id] assoc :bets rep-bets :status status :stack (- stack bet))
        (assoc :min-raise bet)
        (update :next-events
                conj
                [:game-event/count-pot {}]
                [:game-event/next-player-or-stage {:player-id player-id}])
        (assoc :player-action
               [:player-action/bet {:player-id player-id, :bet bet, :all-in? all-in?}]))))

;; RAISE
;; the raise must great than (max min-raise BB)
;; the raise must less equal than stack
;; if raise equals stack, means all-in
;; TODO can't raise when all player all-in
(defmethod apply-game-event :game-event/player-raise
  [game-state
   [_ {:keys [player-id raise]}]]
  (let [{:keys [players street-bet min-raise]} game-state
        {:keys [bets stack]} (get players player-id)
        bet      (peek bets)
        all-in?  (= stack raise)
        status   (if all-in? :player-status/all-in :player-status/acted)
        new-bet  (+ (or bet 0) raise)
        rep-bets (conj (pop bets) new-bet)]
    (-> game-state
        (update-in [:players player-id] assoc :status status :bets rep-bets :stack (- stack raise))
        (assoc :min-raise ((fnil max 0) min-raise (- new-bet (or street-bet 0))))
        (update :next-events
                conj
                [:game-event/count-pot {}]
                [:game-event/next-player-or-stage {:player-id player-id}])
        (assoc :player-action
               [:player-action/raise {:player-id player-id, :raise raise, :all-in? all-in?}]))))

;; FOLD
(defmethod apply-game-event :game-event/player-fold
  [game-state
   [_ {:keys [player-id]}]]
  (-> game-state
      (update-in [:players player-id] assoc :status :player-status/fold)
      (update :next-events
              conj
              [:game-event/count-pot {}]
              [:game-event/next-player-or-stage {:player-id player-id}])
      (assoc :player-action [:player-action/fold {:player-id player-id}])))

;; System fold
(defmethod apply-game-event :game-event/fold-player
  [game-state
   [_ {:keys [player-id]}]]
  (-> game-state
      (update-in [:players player-id] assoc :status :player-status/fold)
      (update :next-events
              conj
              [:game-event/count-pot {}]
              [:game-event/next-player-or-stage {:player-id player-id}])
      (assoc :player-action [:player-action/fold {:player-id player-id}])))

(defmethod apply-game-event :game-event/player-check
  [game-state
   [_ {:keys [player-id]}]]
  (-> game-state
      (update-in [:players player-id] assoc :status :player-status/acted)
      (update :next-events
              conj
              [:game-event/next-player-or-stage {:player-id player-id}])
      (assoc :player-action [:player-action/check {:player-id player-id}])))


;; Player can musk his cards before showdown.
(defmethod apply-game-event :game-event/player-musk
  [game-state [_ {:keys [player-id]}]]
  (-> game-state
      (update-in [:players player-id] assoc :status :player-status/fold)
      (assoc :player-action [:player-action/musk {:player-id player-id}])))

;; If a player is the last player, he will win all pots
(defmethod apply-game-event :game-event/last-player
  [game-state [_ {:keys [player-id]}]]
  (let [{:keys [pots players]} game-state
        award       (transduce (map :value) + pots)
        awards      {player-id award}
        rep-pots    (map #(assoc % :winner-ids [player-id]) pots)
        rep-players (misc/reward-to-players awards players)]
    (-> game-state
        (assoc :status             :game-status/last-player-settlement
               :in-game-player-ids []
               :cards              nil
               :pots               rep-pots
               :street-bet         nil
               :community-cards    []
               :min-raise          nil
               :showdown           nil
               :return-bets        nil
               :players            rep-players
               :awards             awards)
        (update :schedule-events
                conj
                (model/make-schedule-event {:timeout 4000,
                                            :event   [:game-event/next-game {}]})))))

(defmethod apply-game-event :game-event/showdown-prepare
  [game-state _]
  (-> game-state
      (assoc :status :game-status/showdown-prepare)
      (update :schedule-events
              conj
              (model/make-schedule-event {:timeout 2000,
                                          :event   [:game-event/showdown {}]}))))

(defmethod apply-game-event :game-event/showdown
  [game-state _]
  (let [{:keys [pots players community-cards in-game-player-ids]} game-state
        {:keys [return-bets remain-pots]} (impl/pots-return-bets pots)

        players (map-vals (fn [p]
                            (update p :stack + (get return-bets (:id p) 0)))
                          players)

        ;; showdown: id -> card value
        {:keys [showdown awards pots]} (misc/calc-showdown {:players            players,
                                                            :pots               remain-pots,
                                                            :in-game-player-ids in-game-player-ids,
                                                            :community-cards    community-cards})

        rep-players (misc/reward-to-players awards players)]
    (-> game-state
        (assoc :showdown           showdown
               :status             :game-status/showdown-settlement
               :in-game-player-ids []
               :cards              nil
               :pots               pots
               :street-bet         nil
               :min-raise          nil
               :return-bets        return-bets
               :players            rep-players
               :awards             awards)
        (update :schedule-events
                conj
                (model/make-schedule-event {:timeout 10000,
                                            :event   [:game-event/next-game {}]})))))

;; When all player all-in
;; reward those pots with single contributor.
;; ask players for times to run
(defmethod apply-game-event :game-event/runner-prepare
  [game-state _]
  (let [{:keys [players pots]} game-state
        {:keys [return-bets remain-pots]} (impl/pots-return-bets pots)
        rep-pots    (misc/collapse-pots nil remain-pots)
        rep-players (reduce (fn [ps [id v]] (update-in ps [id :stack] + v)) players return-bets)
        showdown    (map-vals (fn [p] (select-keys p [:hole-cards])) rep-players)
        runner      {:showdown showdown}]
    (-> game-state
        (assoc :players     rep-players
               :pots        rep-pots
               :runner      runner
               :return-bets return-bets
               :status      :game-status/runner-prepare)
        (update :schedule-events
                conj
                (model/make-schedule-event {:timeout 4000,
                                            :event   [:game-event/runner-run {}]})))))

(defmethod apply-game-event :game-event/runner-run
  [game-state _]
  (let [{:keys [players community-cards runner cards pots]} game-state
        {:keys [deal-times]} runner
        ;; when no player request deal times, default to 1
        deal-times  (or deal-times 1)
        deal-n      (- 5 (count community-cards))
        deals-list  (->> cards
                         (partition-all deal-n)
                         (take deal-times))
        undivided-pot-values (mapv (fn [pot] (mod (:value pot) deal-times)) pots)
        runner-pots (mapv (fn [pot] (update pot :value quot deal-times)) pots)
        results     (->> deals-list
                         (map-indexed
                          (fn [idx deals]
                            (let [cards       (into community-cards deals)
                                  runner-pots (if (zero? idx)
                                                (mapv #(update %1 :value + %2)
                                                      runner-pots
                                                      undivided-pot-values)
                                                runner-pots)
                                  {:keys [awards pots showdown]} (misc/calc-showdown
                                                                  {:players         players,
                                                                   :pots            runner-pots,
                                                                   :community-cards cards})]
                              {:awards          awards,
                               :pots            pots,
                               :showdown        showdown,
                               :community-cards cards}))))
        awards      (apply merge-with
                           (fn [& xs]
                             (->> xs
                                  (filter pos?)
                                  (reduce +)))
                           (map :awards results))
        rep-players (misc/reward-to-players awards players)]
    (-> game-state
        (update :runner assoc :results results)
        (assoc :awards             awards
               :players            rep-players
               :status             :game-status/runner-settlement
               :in-game-player-ids []
               :cards              nil
               :street-bet         nil
               :community-cards    []
               :min-raise          nil
               :showdown           nil)
        (update :schedule-events
                conj
                (model/make-schedule-event {:timeout (case deal-times
                                                       1 13000
                                                       2 23000),
                                            :event   [:game-event/next-game {}]})))))

(defmethod apply-game-event :game-event/player-request-deal-times
  [game-state
   [_ {:keys [player-id deal-times]}]]
  (if (seq (get-in game-state [:runner :player-deal-times]))
    (-> game-state
        (update-in [:runner :deal-times] (fnil min 2) deal-times)
        (assoc-in [:runner :player-deal-times player-id] deal-times)
        (assoc :player-action
               [:player-action/player-request-deal-times
                {:player-id player-id, :deal-times deal-times}]))
    (-> game-state
        (assoc-in [:runner :deal-times] (min 2 deal-times))
        (assoc-in [:runner :player-deal-times player-id] deal-times)
        (assoc :player-action
               [:player-action/player-request-deal-times
                {:player-id player-id, :deal-times deal-times}]))))

(defmethod apply-game-event :game-event/player-get-current-state
  [game-state [_ _]]
  game-state)
