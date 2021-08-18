(ns poker.events.game
  "Game state events."
  (:require
   [re-frame.core :as re-frame]
   [poker.events.sound]
   [poker.utils   :as u]
   [poker.logger  :as log]))

;; Server events

(defmulti merge-server-state
  "Client side server state transformer.
  reconstruct partial local game state for client-side.

  Return {local-state events}:
  local state will be updated if local-state is a present.
  events is a list of {delay event params}.
  events will be dispatch as [event (assoc params :game/id game-id)] after delay milliseconds.

  By default, return nil.
  "
  (fn [event _old-game-state _game-state]
    (first event)))

(defmethod merge-server-state :default
  [_event _old-game-state _game-state]
  nil)

(defmethod merge-server-state :game-event/next-game
  [_event _old-game-state _game-state]
  {:local-state {}})

(defmethod merge-server-state :game-event/runner-prepare
  [_event old-game-state game-state]
  (let [{:keys [runner return-bets]} game-state
        rep-players (->> (:players old-game-state)
                         (u/map-vals
                          (fn [{:keys [id], :as p}]
                            (cond-> p
                              (get-in runner [:showdown id])
                              (assoc :showdown-cards
                                     (get-in runner [:showdown id :hole-cards]))
                              (get return-bets id)
                              (update :stack + (get return-bets id))))))]
    {:local-state {:players     rep-players,
                   :has-all-in? true}}))

(defmethod merge-server-state :game-event/player-raise
  [_event _old-game-state game-state]
  (if (some #(= :player-status/all-in (:status %)) (vals (:players game-state)))
    {:local-state {:has-all-in? true}}
    {}))

(defmethod merge-server-state :game-event/player-bet
  [_event _old-game-state game-state]
  (if (some #(= :player-status/all-in (:status %)) (vals (:players game-state)))
    {:local-state {:has-all-in? true}}
    {}))

;; I. Runner
(defmethod merge-server-state :game-event/runner-run
  [_event old-game-state game-state]
  (let [{:keys [runner]}  game-state
        {:keys [players]} old-game-state
        results           (:results runner)
        old-community-cards (:community-cards old-game-state)
        players-1         (->> (:players old-game-state)
                               (u/map-vals
                                (fn [{:keys [id], :as p}]
                                  (cond-> p
                                    (get-in runner [:showdown id])
                                    (assoc :showdown-cards
                                           (get-in runner [:showdown id :hole-cards]))))))
        events            (->>
                           results
                           (reduce
                            (fn [[idx events last-players] {:keys [awards showdown pots], :as rst}]
                              (let [winner-id-set (->> pots
                                                       (filter #(< 1 (count (:player-ids %))))
                                                       (mapcat (comp vec :winner-ids))
                                                       (set))

                                    players-1     (->>
                                                   (or last-players players)
                                                   (u/map-vals
                                                    (fn [{:keys [id], :as p}]
                                                      (assoc p
                                                             :bets           nil
                                                             ;; reset the player status
                                                             :status         (get-in players
                                                                                     [id :status])
                                                             :showdown-cards (get-in
                                                                              showdown
                                                                              [id
                                                                               :hole-cards])))))
                                    players-2     (->>
                                                   players-1
                                                   (u/map-vals
                                                    (fn [{:keys [id], :as p}]
                                                      (let [v (get awards id)]
                                                        (cond-> p
                                                          v
                                                          (update :stack + v)

                                                          (winner-id-set id)
                                                          (assoc :status
                                                                 :player-status-local/winner))))))]
                                [(inc idx)
                                 (conj events
                                       {:delay  (* idx 9000),
                                        :event  :game/runner-next-result,
                                        :params {:result       (-> rst
                                                                   (assoc :players players-1)
                                                                   (dissoc :community-cards)
                                                                   (assoc :winner-awards nil)),
                                                 :runner-cards (:community-cards rst)}}
                                       {:delay  (+ (* idx 9000) 7000),
                                        :event  :game/local-winner-awards,
                                        :params {:awards awards, :players players-2}}
                                       {:delay  (+ (* idx 9000) 7000),
                                        :event  :history/new-record,
                                        :params {:showdown showdown,
                                                 :awards   awards}})

                                 players-2]))
                            [0 [] nil])
                           (second))
        local-state       {:players         players-1,
                           :pots            nil,
                           :community-cards old-community-cards,
                           :has-all-in?     true}]
    (log/info "shallow local state:" local-state)
    (log/info "schedule local events:" events)
    {:local-state local-state,
     :events      events}))

;; II. One Player Left
;; Keep the old player states for 1 second.
;; Shallow with new player states with winner status.
(defmethod merge-server-state :game-event/last-player
  [_event old-game-state game-state]
  (let [{:keys [awards players]} game-state
        players-1   (:players old-game-state)
        players-2   (->> players
                         (u/map-vals (fn [{:keys [id], :as p}]
                                       (cond-> (assoc p :bets nil)
                                         (get awards id)
                                         (assoc :status :player-status-local/winner)))))
        events      [{:delay  1000,
                      :event  :game/local-winner-awards,
                      :params {:awards awards, :players players-2}}
                     {:delay  1000,
                      :event  :history/new-record,
                      :params {:street (name (:status old-game-state))
                               :awards awards}}]
        local-state {:players players-1}]
    (log/info "shallow local state:" local-state)
    (log/info "schedule local events:" events)
    {:local-state local-state,
     :events      events}))

;; III. Showdown
;; Keep the origin stack display during showdown
;; Display the winner after 3 seconds.
(defmethod merge-server-state :game-event/showdown
  [_event old-game-state game-state]
  (let [{:keys [awards showdown players]} game-state
        players-1   (->> (:players old-game-state)
                         (u/map-vals
                          (fn [{:keys [id], :as p}]
                            (cond-> p
                              (get showdown id)
                              (assoc :showdown-cards (get-in showdown [id :hole-cards]))))))
        players-2   (->> players
                         (u/map-vals
                          (fn [{:keys [id], :as p}]
                            (cond-> (assoc p :bets nil)
                              (get awards id)
                              (assoc :status :player-status-local/winner)

                              (get showdown id)
                              (assoc :showdown-cards (get-in showdown [id :hole-cards]))))))
        events      [{:delay  3000,
                      :event  :game/local-winner-awards,
                      :params {:awards awards, :players players-2}}
                     {:delay  3000,
                      :event  :history/new-record,
                      :params {:showdown showdown,
                               :awards   awards}}]
        local-state {:players players-1}]
    (log/info "shallow local state:" local-state)
    (log/info "schedule local events:" events)
    {:local-state local-state,
     :events      events}))

(defmethod merge-server-state :game-event/player-request-deal-times
  [event _old-game-state _game-state]
  (let [[_ req] event]
    {:events [{:delay  10,
               :event  :chat/system-player-request-deal-times,
               :params req}]}))

;; Server push events

(re-frame/reg-event-fx :game-server-event/game-state-updated
  (fn [{:keys [db]} [_ {:game/keys [event state id event-id]}]]
    (let [last-event-id  (or (get-in db [:game/last-event-ids id]) 0)

          {:keys [local-state events]} (merge-server-state event
                                                           (get-in db [:game/states id])
                                                           state)
          dispatch-later (for [{:keys [event delay params]} events]
                           {:ms       delay,
                            :dispatch [event (assoc params :game/id id)]})]
      (log/info "receive game state, this event:" event "event-id" event-id)
      (if (> event-id last-event-id)
        (cond-> {:db (-> db
                         (assoc-in [:game/states id] state)
                         (assoc-in [:game/last-event-ids id] event-id))}
          ;; Only play sound in current game
          (= (:game/id db) id)
          (assoc :sound/play-event-sound
                 {:state state,
                  :event event})
          ;; shallow game states with local-states
          local-state
          (assoc-in [:db :game/local-states id] local-state)
          ;; dispatch these events later
          dispatch-later
          (assoc :dispatch-later dispatch-later))
        {:db db}))))

;; Client local scheduled events

(re-frame/reg-event-fx :game/local-winner-awards
  (fn [{:keys [db]} [_ {:keys [awards players], :game/keys [id]}]]
    (cond->
      {:db (-> db
               (assoc-in [:game/local-states id :winner-awards] awards)
               (assoc-in [:game/local-states id :players] players))}
      (= (:game/id db) id)
      (assoc :sound/play-event-sound {:event [:local/winner]}))))

(re-frame/reg-event-db :game/flush-local-state
  (fn [db [_ {:game/keys [id]}]]
    (assoc-in db [:game/local-states id] {})))

(re-frame/reg-event-db :game/runner-next-result
  (fn [db [_ {:keys [result runner-cards], :game/keys [id]}]]
    (-> db
        (update-in [:game/local-states id] merge result)
        (update-in [:game/local-states id :runner-cards-deals] (fnil conj []) runner-cards))))

(re-frame/reg-event-db :game/local-players-add-stack
  (fn [db [_ {:keys [awards], :game/keys [id]}]]
    (let [{:keys [players]} (get-in db [:game/local-state id])
          rep-players       (u/map-vals (fn [p] (update p :stack + (get awards (:id p) 0)))
                                        players)]
      (assoc-in db [:game/local-state id :players] rep-players))))

;; Player actions

(re-frame/reg-event-fx :game/buyin
  (fn [{:keys [db]} [_ {:keys [seat]}]]
    {:db       db,
     :api/send {:event [:game/buyin
                        {:seat    seat,
                         :game-id (:game/id db)}]}}))

(re-frame/reg-event-fx :game/join-bet
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event [:game/join-bet {:game-id (:game/id db)}]}}))

(re-frame/reg-event-fx :game/call
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event [:game/call {:game-id (:game/id db)}]}}))

(re-frame/reg-event-fx :game/bet
  (fn [{:keys [db]} [_ {:keys [bet]}]]
    {:db       db,
     :api/send {:event [:game/bet
                        {:game-id (:game/id db),
                         :bet     bet}]}}))

(re-frame/reg-event-fx :game/check
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event [:game/check {:game-id (:game/id db)}]}}))

(re-frame/reg-event-fx :game/fold
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event [:game/fold {:game-id (:game/id db)}]}}))

(re-frame/reg-event-fx :game/musk
  (fn [{:keys [db]} _]
    {:db       db,
     :api/send {:event [:game/musk {:game-id (:game/id db)}]}}))

(re-frame/reg-event-fx :game/raise
  (fn [{:keys [db]} [_ {:keys [raise]}]]
    {:db       db,
     :api/send {:event [:game/raise
                        {:game-id (:game/id db),
                         :raise   raise}]}}))

(re-frame/reg-event-fx :game/request-deal-times
  (fn [{:keys [db]} [_ {:keys [deal-times]}]]
    {:db       db,
     :api/send {:event [:game/request-deal-times
                        {:game-id    (:game/id db),
                         :deal-times deal-times}]}}))

(re-frame/reg-event-fx :game/get-current-state
  (fn [{:keys [db]} [_ {:keys [game-id]}]]
    {:db       db,
     :api/send {:event [:game/get-current-state
                        {:game-id           game-id,
                         :get-current-state {}}]}}))
