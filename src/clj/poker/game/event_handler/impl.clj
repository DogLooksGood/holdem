(ns poker.game.event-handler.impl
  (:require
   [clojure.tools.logging :as log]))

(defn add-player
  [state {:keys [id], :as player}]
  ;; only leaved player can be found here.
  ;; he can continue the game in next round
  (let [p (get-in state [:players id])]
    (if p
      (update-in state [:players id] assoc :status :player-status/wait-for-start)
      (update state :players assoc id player))))

(defn remove-player
  [state id]
  (let [player (get-in state [:players id])
        {:keys [stack seat]} player]
    (log/debugf "Remove player: %s" id)
    (-> state
        (update :players dissoc id)
        (update :seats dissoc seat)
        (update :ladder-events
                conj
                [:ladder-event/player-returns {:player-id id, :returns stack}]))))

(defn player-returns
  [state id returns]
  (let [{:keys [stack]} (get-in state [:players id])
        remain-stack    (- stack returns)]
    (-> state
        (update :ladder-events
                conj
                [:ladder-event/player-returns {:player-id id, :returns returns}])
        (assoc-in [:players id :stack] remain-stack))))

(defn mark-player-dropout
  [state id]
  (assoc-in state [:players id :network-error] :network-error/dropout))

(defn mark-player-join-bet
  [state id]
  (assoc-in state [:players id :status] :player-status/join-bet))

(defn mark-player-leave
  [state id]
  (let [player (get-in state [:players id])]
    (log/debugf "Mark player status leave: %s" id)
    (cond-> (assoc-in state [:players id :status] :player-status/leave)
      (= :player-status/in-action (:status player))
      (update :next-events
              conj
              [:game-event/count-pot {}]
              [:game-event/next-player-or-stage {:player-id id}]))))

(defn set-player-off-seat
  [state id]
  (let [player         (get-in state [:players id])
        {:keys [seat]} player]
    (cond-> state
      true
      (update-in [:players id]
                 assoc
                 :status     :player-status/off-seat
                 :bets       nil
                 :hole-cards nil
                 :position   nil
                 :stack      0
                 :seat       nil)
      true
      (update :ladder-events conj [:ladder-event/player-inc-hand {:player-id id}])
      seat
      (update :seats dissoc seat))))

(defn reset-player-state-for-new-game
  [state id]
  (let [p     (get-in state [:players id])
        rep-p (assoc p
                     :status     :player-status/wait-for-start
                     :bets       nil
                     :hole-cards nil
                     :prev-stack (:stack p)
                     :position   nil)]
    (-> state
        (assoc-in [:players id] rep-p)
        (update :ladder-events conj [:ladder-event/player-inc-hand {:player-id id}]))))

(defn unmark-player-network-error
  [state id]
  (assoc-in state [:players id :network-error] nil))

(defn player-set-stack
  [state id stack]
  (-> state
      (update-in [:players id] assoc :stack stack :prev-stack stack)))

(defn player-set-seat
  [state id seat]
  (-> state
      (update-in [:players id] assoc :status :player-status/wait-for-bb :seat seat)
      (update :seats assoc seat id)))

(defn pots-return-bets
  [pots]
  (let [{reward-pots true, remain-pots false}
        (->> pots
             (group-by #(= 1 (count (:player-ids %)))))

        return-bets (reduce (fn [acc {:keys [player-ids value]}]
                              (update acc (first player-ids) (fnil + 0) value))
                            {}
                            reward-pots)]
    {:return-bets return-bets,
     :remain-pots remain-pots}))
