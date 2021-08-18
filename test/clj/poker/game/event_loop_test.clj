(ns poker.game.event-loop-test
  (:require
   [poker.game.model      :as model]
   [poker.game.misc       :as misc]
   [poker.game.event-loop :as sut]
   [poker.game.engine     :as engine]
   [poker.game.protocols  :as p]
   [clojure.tools.logging :as log]
   [clojure.test          :as t]))

(defmacro with-fixed-random-seed
  [& body]
  `(try (alter-var-root #'misc/*cards-shuffle-seed* (constantly 1))
        (do ~@body)
        (finally (alter-var-root #'misc/*cards-shuffle-seed* (constantly nil)))))

;; A regular game with 3 players
(t/deftest event-loop-1
  (with-fixed-random-seed
   (let [state* (atom (engine/make-game-engine {:id 1 :name "test"} engine/default-game-opts))
         [c1 c2 c3 c4 c5 c6 c7 c8 c9 c10 c11] (misc/create-deck-of-cards)]
     (t/testing "player join & buyin"
       (log/info "player join & buyin, player 2 SB, player 3 BB")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-join {:id 1, :name "foo"}]
                           [:game-event/player-join {:id 2, :name "bar"}]
                           [:game-event/player-join {:id 3, :name "baz"}]
                           [:game-event/player-buyin {:player-id 1, :seat 1, :stack-add 10000}]
                           [:game-event/player-buyin
                            {:player-id 2, :seat 2, :stack-add 10000, :no-auto-start? true}]
                           [:game-event/player-buyin
                            {:player-id 3, :seat 3, :stack-add 10000, :no-auto-start? true}]
                           [:game-event/start-game {}])
         (reset! state* (sut/freeze-state! el)))
       (t/is (= {:status             :game-status/preflop,
                 :community-cards    [],
                 :pots               [(model/make-pot {:player-ids #{2 3},
                                                       :value      200,
                                                       :street     :preflop})
                                      (model/make-pot {:player-ids #{3},
                                                       :value      100,
                                                       :street     :preflop})],
                 :in-game-player-ids [2 3 1],
                 :players            {1 (model/make-player-state
                                         {:id         1,
                                          :status     :player-status/in-action,
                                          :bets       [nil],
                                          :stack      10000,
                                          :seat       1,
                                          :name       "foo",
                                          :position   "BTN",
                                          :hole-cards [c5 c6]}),
                                      2 (model/make-player-state
                                         {:id         2,
                                          :stack      9900,
                                          :bets       [100],
                                          :seat       2,
                                          :name       "bar",
                                          :status     :player-status/wait-for-action,
                                          :position   "SB",
                                          :hole-cards [c1 c2]}),
                                      3 (model/make-player-state
                                         {:id         3,
                                          :stack      9800,
                                          :bets       [200],
                                          :seat       3,
                                          :name       "baz",
                                          :status     :player-status/wait-for-action,
                                          :position   "BB",
                                          :hole-cards [c3 c4]})}}
                (-> (select-keys @state*
                                 [:status :players :pots :in-game-player-ids :community-cards])))))
     (t/testing "player all-in"
       (log/info "player 1 raise to 10k, player 2 call, player 3 fold. final state dropped.")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-raise {:player-id 1, :raise 10000}]
                           [:game-event/player-call {:player-id 2}]
                           [:game-event/player-fold {:player-id 3}])
         (t/is (= {:street-bet      10000,
                   :pots            [(model/make-pot {:player-ids #{1 2},
                                                      :value      20200,
                                                      :street     :preflop})],
                   :status          :game-status/runner-prepare,
                   :community-cards [],
                   :players         {1 (model/make-player-state {:id         1,
                                                                 :status     :player-status/all-in,
                                                                 :bets       [10000],
                                                                 :stack      0,
                                                                 :seat       1,
                                                                 :name       "foo",
                                                                 :position   "BTN",
                                                                 :hole-cards [c5 c6]}),
                                     2 (model/make-player-state {:id         2,
                                                                 :stack      0,
                                                                 :bets       [10000],
                                                                 :seat       2,
                                                                 :name       "bar",
                                                                 :status     :player-status/all-in,
                                                                 :position   "SB",
                                                                 :hole-cards [c1 c2]}),
                                     3 (model/make-player-state {:id         3,
                                                                 :stack      9800,
                                                                 :bets       [200],
                                                                 :seat       3,
                                                                 :name       "baz",
                                                                 :status     :player-status/fold,
                                                                 :position   "BB",
                                                                 :hole-cards [c3 c4]})}}
                  (select-keys (sut/freeze-state! el)
                               [:status :community-cards :players :pots :street-bet])))))
     (t/testing "preflop to flop"
       (log/info
        "preflop to flop: player 1 call, player 2 fold, player 3 raise to 400, player 1 call. final state saved.")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-call {:player-id 1}]
                           [:game-event/player-fold {:player-id 2}]
                           [:game-event/player-raise {:player-id 3, :raise 400}]
                           [:game-event/player-call {:player-id 1}])
         (reset! state* (sut/freeze-state! el)))
       (t/is (= {:status          :game-status/flop,
                 :next-events     [],
                 :pots            [(model/make-pot {:player-ids #{1 3},
                                                    :value      1300,
                                                    :street     :preflop})],
                 :community-cards [c7 c8 c9],
                 :players         {1 (model/make-player-state
                                      {:id         1,
                                       :status     :player-status/wait-for-action,
                                       :bets       [600 nil],
                                       :stack      9400,
                                       :seat       1,
                                       :name       "foo",
                                       :position   "BTN",
                                       :hole-cards [c5 c6]}),
                                   2 (model/make-player-state
                                      {:id         2,
                                       :stack      9900,
                                       :bets       [100 nil],
                                       :seat       2,
                                       :name       "bar",
                                       :status     :player-status/fold,
                                       :position   "SB",
                                       :hole-cards [c1 c2]}),
                                   3 (model/make-player-state
                                      {:id         3,
                                       :stack      9400,
                                       :bets       [600 nil],
                                       :seat       3,
                                       :name       "baz",
                                       :status     :player-status/in-action,
                                       :position   "BB",
                                       :hole-cards [c3 c4]})}}
                (select-keys @state* [:players :community-cards :pots :status :next-events]))))
     (t/testing "flop to turn"
       (log/info "player 3 bet 300, player 1 call. final state saved.")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-bet {:player-id 3, :bet 300}]
                           [:game-event/player-call {:player-id 1}])
         (reset! state* (sut/freeze-state! el)))
       (t/is (= {:status          :game-status/turn,
                 :next-events     [],
                 :community-cards [c7 c8 c9 c10],
                 :players         {1 (model/make-player-state
                                      {:id         1,
                                       :status     :player-status/wait-for-action,
                                       :bets       [600 300 nil],
                                       :stack      9100,
                                       :seat       1,
                                       :name       "foo",
                                       :position   "BTN",
                                       :hole-cards [c5 c6]}),
                                   2 (model/make-player-state {:id         2,
                                                               :stack      9900,
                                                               :bets       [100 nil nil],
                                                               :seat       2,
                                                               :name       "bar",
                                                               :status     :player-status/fold,
                                                               :position   "SB",
                                                               :hole-cards [c1 c2]}),
                                   3 (model/make-player-state {:id         3,
                                                               :stack      9100,
                                                               :bets       [600 300 nil],
                                                               :seat       3,
                                                               :name       "baz",
                                                               :status     :player-status/in-action,
                                                               :position   "BB",
                                                               :hole-cards [c3 c4]})}}
                (select-keys @state* [:players :community-cards :status :next-events]))))
     (t/testing "turn to river"
       (log/info "turn to river, both player check. final state saved.")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-check {:player-id 3}]
                           [:game-event/player-check {:player-id 1}])
         (reset! state* (sut/freeze-state! el)))
       (t/is (= {:status          :game-status/river,
                 :next-events     [],
                 :pots            [(model/make-pot {:player-ids #{1 3},
                                                    :value      1900,
                                                    :street     :flop})],
                 :community-cards [c7 c8 c9 c10 c11],
                 :players         {1 (model/make-player-state
                                      {:id         1,
                                       :status     :player-status/wait-for-action,
                                       :bets       [600 300 nil nil],
                                       :stack      9100,
                                       :seat       1,
                                       :name       "foo",
                                       :position   "BTN",
                                       :hole-cards [c5 c6]}),
                                   2 (model/make-player-state
                                      {:id         2,
                                       :stack      9900,
                                       :bets       [100 nil nil nil],
                                       :seat       2,
                                       :name       "bar",
                                       :status     :player-status/fold,
                                       :position   "SB",
                                       :hole-cards [c1 c2]}),
                                   3 (model/make-player-state
                                      {:id         3,
                                       :stack      9100,
                                       :bets       [600 300 nil nil],
                                       :seat       3,
                                       :name       "baz",
                                       :status     :player-status/in-action,
                                       :position   "BB",
                                       :hole-cards [c3 c4]})}}
                (select-keys @state* [:players :community-cards :pots :status :next-events]))))
     (t/testing "river showdown"
       (log/infof "river showdown")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-check {:player-id 3}]
                           [:game-event/player-check {:player-id 1}])
         (t/is (= {:status          :game-status/settlement,
                   :pots            [(model/make-pot {:player-ids #{1 3},
                                                      :value      1900,
                                                      :winner-ids [3],
                                                      :street     :flop})],
                   :next-events     [],
                   :community-cards [],
                   :awards          {3 1900},
                   :players         {1 (model/make-player-state
                                        {:id         1,
                                         :status     :player-status/wait-for-start,
                                         :bets       nil,
                                         :stack      9100,
                                         :seat       1,
                                         :name       "foo",
                                         :position   "BTN",
                                         :hole-cards nil}),
                                     2 (model/make-player-state
                                        {:id         2,
                                         :stack      9900,
                                         :bets       nil,
                                         :seat       2,
                                         :name       "bar",
                                         :status     :player-status/wait-for-start,
                                         :position   "SB",
                                         :hole-cards nil}),
                                     3 (model/make-player-state
                                        {:id         3,
                                         :stack      11000,
                                         :bets       nil,
                                         :seat       3,
                                         :name       "baz",
                                         :status     :player-status/wait-for-start,
                                         :position   "BB",
                                         :hole-cards nil})}}
                  (select-keys (sut/freeze-state! el)
                               [:players :community-cards :status :next-events :pots :awards])))))
     (t/testing "one player left"
       (log/info "one player left")
       (let [el (sut/make-event-loop)]
         (p/start el @state*)
         (sut/send-events! el
                           [:game-event/player-bet {:player-id 3, :bet 1000}]
                           [:game-event/player-fold {:player-id 1}])
         (t/is (= {:status          :game-status/settlement,
                   :pots            [(model/make-pot {:player-ids #{3},
                                                      :value      1900,
                                                      :winner-ids [3],
                                                      :street     :flop})
                                     (model/make-pot {:player-ids #{3},
                                                      :value      1000,
                                                      :winner-ids [3],
                                                      :street     :river})],
                   :next-events     [],
                   :community-cards [],
                   :awards          {3 2900},
                   :players         {1 (model/make-player-state
                                        {:id         1,
                                         :status     :player-status/wait-for-start,
                                         :bets       nil,
                                         :stack      9100,
                                         :seat       1,
                                         :name       "foo",
                                         :position   "BTN",
                                         :hole-cards nil}),
                                     2 (model/make-player-state
                                        {:id         2,
                                         :stack      9900,
                                         :bets       nil,
                                         :seat       2,
                                         :name       "bar",
                                         :status     :player-status/wait-for-start,
                                         :position   "SB",
                                         :hole-cards nil}),
                                     3 (model/make-player-state
                                        {:id         3,
                                         :stack      11000,
                                         :bets       nil,
                                         :seat       3,
                                         :name       "baz",
                                         :status     :player-status/wait-for-start,
                                         :position   "BB",
                                         :hole-cards nil})}}
                  (select-keys (sut/freeze-state! el)
                               [:players
                                :community-cards
                                :status
                                :next-events
                                :pots
                                :awards]))))))))
