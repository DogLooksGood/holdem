(ns poker.game.event-handler-test
  (:require
   [poker.game.engine    :as engine]
   [poker.game.misc      :as misc]
   [poker.game.model     :as model]
   [poker.game.protocols :as p]
   [clojure.test         :as t])
  (:import clojure.lang.ExceptionInfo))

(defn- process-event
  [engine event]
  (p/validate-event! engine event)
  (p/apply-event engine event))

(t/deftest player-join
  (t/testing "success"
    (t/is (= {1 (model/make-player-state {:id 1, :name "foo", :status :player-status/off-seat})}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (process-event [:game-event/player-join {:id 1, :name "foo"}])
                 (:players))))))

(t/deftest player-leave
  (t/testing "success"
    (t/is (= {}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :players {1 (model/make-player-state {:id 1, :name "foo"})})
                 (process-event [:game-event/player-leave {:player-id 1}])
                 (:players))))))

(t/deftest player-buyin
  (t/testing "success"
    (t/is
     (= {1 (model/make-player-state
            {:id         1,
             :name       "foo",
             :seat       1,
             :status     :player-status/wait-for-bb,
             :stack      20000,
             :prev-stack 20000})}
        (-> (engine/make-game-engine {} engine/default-game-opts)
            (assoc :players {1 (model/make-player-state {:id 1, :name "foo"})})
            (process-event [:game-event/player-buyin {:player-id 1, :seat 1, :stack-add 20000}])
            (:players)))))
  (t/testing "less than min buyin"
    (t/is
     (thrown-with-msg? ExceptionInfo #"Less than min buyin"
       (-> (engine/make-game-engine {} engine/default-game-opts)
           (assoc :players {1 (model/make-player-state {:id 1, :name "foo"})})
           (process-event [:game-event/player-buyin {:player-id 1, :seat 1, :stack-add 3000}])
           (:players)))))
  (t/testing "great than min buyin"
    (t/is
     (thrown-with-msg? ExceptionInfo #"Great than max buyin"
       (-> (engine/make-game-engine {} engine/default-game-opts)
           (assoc :players {1 (model/make-player-state {:id 1, :name "foo"})})
           (process-event [:game-event/player-buyin {:player-id 1, :seat 1, :stack-add 21000}])
           (:players)))))
  (t/testing "invalid player id"
    (t/is
     (thrown-with-msg? ExceptionInfo #"Invalid player id"
       (-> (engine/make-game-engine {} engine/default-game-opts)
           (process-event [:game-event/player-buyin {:player-id 1, :seat 1, :stack-add 20000}])
           (:players)))))
  (t/testing "seat is occupied"
    (t/is
     (thrown-with-msg? ExceptionInfo #"Seat is occupied"
       (-> (engine/make-game-engine {} engine/default-game-opts)
           (assoc :players
                  {1 (model/make-player-state {:id 1, :name "foo"}),
                   2 (model/make-player-state {:id 1, :name "foo", :seat 1})}
                  :seats
                  {1 2})
           (process-event [:game-event/player-buyin {:player-id 1, :seat 1, :stack-add 20000}])
           (:players))))))

(t/deftest start-game
  (t/testing "success"
    (t/is (= {:next-events [[:game-event/collect-players {}] [:game-event/deal-cards {}]],
              :status      :game-status/preflop}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :btn-seat 1
                        :players  {1 (model/make-player-state
                                      {:id     1,
                                       :name   "foo",
                                       :seat   1,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   2 (model/make-player-state
                                      {:id     2,
                                       :seat   2,
                                       :stack  20000,
                                       :status :player-status/wait-for-start})})
                 (process-event [:game-event/start-game {}])
                 (select-keys [:next-events :status])))))
  (t/testing "no enough players"
    (t/is (thrown-with-msg? ExceptionInfo #"No enough players"
            (-> (engine/make-game-engine {} engine/default-game-opts)
                (assoc :players
                       {1 (model/make-player-state
                           {:id 1, :seat 1, :stack 20000, :status :player-status/wait-for-start})})
                (process-event [:game-event/start-game {}]))))))


(t/deftest collect-players
  (t/testing "two players"
    (t/is (= {:next-events        [[:game-event/blind-bet {:player-id 2, :bet 100}]
                                   [:game-event/blind-bet {:player-id 1, :bet 200}]
                                   [:game-event/count-pot {}]
                                   [:game-event/next-player-or-stage {:player-id 1}]],
              :in-game-player-ids [1 2],
              :players            {1 (model/make-player-state
                                      {:id       1,
                                       :seat     1,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "BB",
                                       :status   :player-status/wait-for-action}),
                                   2 (model/make-player-state
                                      {:id       2,
                                       :seat     2,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "BTN SB",
                                       :status   :player-status/wait-for-action})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :btn-seat 1
                        :players  {1 (model/make-player-state
                                      {:id     1,
                                       :seat   1,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   2 (model/make-player-state
                                      {:id     2,
                                       :seat   2,
                                       :stack  20000,
                                       :status :player-status/wait-for-start})})
                 (process-event [:game-event/collect-players {}])
                 (select-keys [:players :in-game-player-ids :next-events])))))
  (t/testing "new player at BB position"
    (t/is (= {:next-events        [[:game-event/blind-bet {:player-id 3, :bet 100}]
                                   [:game-event/blind-bet {:player-id 4, :bet 200}]
                                   [:game-event/count-pot {}]
                                   [:game-event/next-player-or-stage {:player-id 4}]],
              :in-game-player-ids [3 4 1 2],
              :players            {1 (model/make-player-state
                                      {:id       1,
                                       :seat     1,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "CO",
                                       :status   :player-status/wait-for-action}),
                                   2 (model/make-player-state
                                      {:id       2,
                                       :seat     2,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "BTN",
                                       :status   :player-status/wait-for-action}),
                                   3 (model/make-player-state
                                      {:id       3,
                                       :seat     3,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "SB",
                                       :status   :player-status/wait-for-action}),
                                   4 (model/make-player-state
                                      {:id       4,
                                       :seat     4,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "BB",
                                       :status   :player-status/wait-for-action})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :btn-seat 1
                        :players  {1 (model/make-player-state
                                      {:id     1,
                                       :seat   1,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   2 (model/make-player-state
                                      {:id     2,
                                       :seat   2,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   3 (model/make-player-state
                                      {:id     3,
                                       :seat   3,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   4 (model/make-player-state
                                      {:id     4,
                                       :seat   4,
                                       :stack  20000,
                                       :status :player-status/wait-for-bb})})
                 (process-event [:game-event/collect-players {}])
                 (select-keys [:players :in-game-player-ids :next-events])))))
  (t/testing "strip player wait for BB"
    (t/is (= {:next-events        [[:game-event/blind-bet {:player-id 4, :bet 100}]
                                   [:game-event/blind-bet {:player-id 1, :bet 200}]
                                   [:game-event/count-pot {}]
                                   [:game-event/next-player-or-stage {:player-id 1}]],
              :in-game-player-ids [4 1 2],
              :players            {1 (model/make-player-state
                                      {:id       1,
                                       :seat     1,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "BB",
                                       :status   :player-status/wait-for-action}),
                                   2 (model/make-player-state
                                      {:id       2,
                                       :seat     2,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "BTN",
                                       :status   :player-status/wait-for-action}),
                                   3 (model/make-player-state {:id     3,
                                                               :seat   3,
                                                               :stack  20000,
                                                               :status :player-status/wait-for-bb}),
                                   4 (model/make-player-state
                                      {:id       4,
                                       :seat     4,
                                       :stack    20000,
                                       :bets     [nil],
                                       :position "SB",
                                       :status   :player-status/wait-for-action})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :btn-seat 1
                        :players  {1 (model/make-player-state
                                      {:id     1,
                                       :seat   1,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   2 (model/make-player-state
                                      {:id     2,
                                       :seat   2,
                                       :stack  20000,
                                       :status :player-status/wait-for-start}),
                                   3 (model/make-player-state {:id     3,
                                                               :seat   3,
                                                               :stack  20000,
                                                               :status :player-status/wait-for-bb}),
                                   4 (model/make-player-state
                                      {:id     4,
                                       :seat   4,
                                       :stack  20000,
                                       :status :player-status/wait-for-start})})
                 (process-event [:game-event/collect-players {}])
                 (select-keys [:players :in-game-player-ids :next-events]))))))

(t/deftest deal-cards
  (t/testing "success"
    ;; use a fixed seed to get a predictable shuffle result
    (binding [misc/*cards-shuffle-seed* 1]
      (let [[c1 c2 c3 c4 & cs] (misc/create-deck-of-cards)]
        (t/is (= {:players {1 (model/make-player-state {:id         1,
                                                        :position   "BB",
                                                        :hole-cards [c1 c2]}),
                            2 (model/make-player-state {:id         2,
                                                        :position   "BTN SB",
                                                        :hole-cards [c3 c4]})},
                  :cards   cs}
                 (-> (engine/make-game-engine {} engine/default-game-opts)
                     (assoc :in-game-player-ids [1 2]
                            :players {1 (model/make-player-state {:id 1, :position "BB"}),
                                      2 (model/make-player-state {:id 2, :position "BTN SB"})})
                     (process-event [:game-event/deal-cards {}])
                     (select-keys [:players :cards]))))))))

(t/deftest count-pot
  (t/testing "success"
    (t/is (= {:street-bet 2000,
              :pots       [(model/make-pot {:value 8500, :player-ids #{1 3}, :street :turn})]}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3 4]
                        :street-bet 200
                        :status     :game-status/river
                        :players    {2 (model/make-player-state {:id     2,
                                                                 :stack  3000,
                                                                 :status :player-status/fold}),
                                     1 (model/make-player-state {:id 1,
                                                                 :stack 3000,
                                                                 :current-bet 2000,
                                                                 :bets [500 1000 2000],
                                                                 :status :player-status/acted}),
                                     3 (model/make-player-state {:id 3,
                                                                 :stack 3000,
                                                                 :current-bet 2000,
                                                                 :bets [500 1000 2000],
                                                                 :status :player-status/acted}),
                                     4 (model/make-player-state {:id     4,
                                                                 :bets   [500 1000],
                                                                 :stack  3000,
                                                                 :status :player-status/fold})})
                 (process-event [:game-event/count-pot {:player-id 2}])
                 (select-keys [:street-bet :pots]))))))

(t/deftest player-call
  (t/testing "success with no bet"
    (t/is (= {:street-bet  200,
              :next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {2 (model/make-player-state
                               {:id 2, :bets [200], :stack 2800, :status :player-status/acted})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 200
                        :status     :game-status/preflop
                        :players    {2 (model/make-player-state
                                        {:id     2,
                                         :stack  3000,
                                         :bets   [nil],
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-call {:player-id 2}])
                 (select-keys [:street-bet :players :next-events])))))
  (t/testing "success with bet"
    (t/is (= {:street-bet  200,
              :next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {2 (model/make-player-state
                               {:id 2, :bets [200], :stack 2900, :status :player-status/acted})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 200
                        :status     :game-status/preflop
                        :players    {2 (model/make-player-state
                                        {:id     2,
                                         :stack  3000,
                                         :bets   [100],
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-call {:player-id 2}])
                 (select-keys [:street-bet :players :next-events])))))
  (t/testing "success call all-in"
    (t/is (= {:street-bet  200,
              :next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {2 (model/make-player-state
                               {:id 2, :bets [200], :stack 0, :status :player-status/all-in})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 200
                        :status     :game-status/preflop
                        :players    {2 (model/make-player-state
                                        {:id     2,
                                         :stack  200,
                                         :bets   [nil],
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-call {:player-id 2}])
                 (select-keys [:street-bet :players :next-events])))))
  (t/testing "player not in action"
    (t/is (thrown-with-msg? ExceptionInfo #"Player not in action"
            (-> (engine/make-game-engine {} engine/default-game-opts)
                (assoc :in-game-player-ids [1 2 3]
                       :street-bet 200
                       :status     :game-status/preflop
                       :players    {2 (model/make-player-state {:id     2,
                                                                :stack  200,
                                                                :status :player-status/in-action}),
                                    3 (model/make-player-state
                                       {:id     2,
                                        :stack  200,
                                        :status :player-status/wait-for-action})})
                (process-event [:game-event/player-call {:player-id 3}])
                (select-keys [:street-bet :players :next-events]))))))

(t/deftest player-bet
  (t/testing "success"
    (t/is (= {:next-events   [[:game-event/count-pot {}]
                              [:game-event/next-player-or-stage {:player-id 2}]],
              :players       {2 (model/make-player-state
                                 {:id 2, :bets [1000], :stack 2000, :status :player-status/acted})},
              :player-action [:player-action/bet {:player-id 2, :bet 1000, :all-in? false}]}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet nil
                        :status     :game-status/preflop
                        :players    {2 (model/make-player-state
                                        {:id     2,
                                         :stack  3000,
                                         :bets   [nil],
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-bet {:player-id 2, :bet 1000}])
                 (select-keys [:players :next-events :player-action])))))
  (t/testing "bet all-in"
    (t/is (= {:next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {2 (model/make-player-state
                               {:id 2, :bets [3000], :stack 0, :status :player-status/all-in})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :status     :game-status/preflop
                        :street-bet nil
                        :players    {2 (model/make-player-state
                                        {:id     2,
                                         :stack  3000,
                                         :bets   [nil],
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-bet {:player-id 2, :bet 3000}])
                 (select-keys [:players :next-events])))))
  (t/testing "no enough stack"
    (t/is (thrown-with-msg? ExceptionInfo #"No enough stack"
            (-> (engine/make-game-engine {} engine/default-game-opts)
                (assoc :in-game-player-ids [1 2 3]
                       :status  :game-status/preflop
                       :players {2 (model/make-player-state {:id     2,
                                                             :stack  3000,
                                                             :bets   [nil],
                                                             :status :player-status/in-action})})
                (process-event [:game-event/player-bet {:player-id 2, :bet 4000}])))))
  (t/testing "bet is too small"
    (t/is (thrown-with-msg? ExceptionInfo #"Bet is too small"
            (-> (engine/make-game-engine {} engine/default-game-opts)
                (assoc :in-game-player-ids [1 2 3]
                       :opts    {:bb 200}
                       :status  :game-status/preflop
                       :players {2 (model/make-player-state {:id     2,
                                                             :stack  3000,
                                                             :bets   [nil],
                                                             :status :player-status/in-action})})
                (process-event [:game-event/player-bet {:player-id 2, :bet 150}]))))))

(t/deftest player-fold
  (t/testing "success"
    (t/is (= {:next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {1 (model/make-player-state
                               {:id 1, :bets [500], :stack 3000, :status :player-status/acted}),
                            2 (model/make-player-state {:id     2,
                                                        :stack  3000,
                                                        :status :player-status/fold})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :status  :game-status/preflop
                        :players {1 (model/make-player-state {:id     1,
                                                              :bets   [500],
                                                              :stack  3000,
                                                              :status :player-status/acted}),
                                  2 (model/make-player-state {:id     2,
                                                              :stack  3000,
                                                              :status :player-status/in-action})})
                 (process-event [:game-event/player-fold {:player-id 2}])
                 (select-keys [:players :next-events]))))))

(t/deftest player-raise
  (t/testing "success"
    (t/is (= {:next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {1 (model/make-player-state
                               {:id 1, :bets [500], :stack 3000, :status :player-status/acted}),
                            2 (model/make-player-state
                               {:id 2, :bets [1700], :stack 1800, :status :player-status/acted})},
              :min-raise   700}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :opts       {:bb 200}
                        :in-game-player-ids [1 2 3]
                        :min-raise  400
                        :street-bet 1000
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :bets   [500],
                                                                 :stack  3000,
                                                                 :status :player-status/acted}),
                                     2 (model/make-player-state
                                        {:id     2,
                                         :bets   [500],
                                         :stack  3000,
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-raise {:player-id 2, :raise 1200}])
                 (select-keys [:players :next-events :min-raise])))))
  (t/testing "success with raise all-in"
    (t/is (= {:next-events [[:game-event/count-pot {}]
                            [:game-event/next-player-or-stage {:player-id 2}]],
              :players     {1 (model/make-player-state
                               {:id 1, :bets [500], :stack 3000, :status :player-status/acted}),
                            2 (model/make-player-state
                               {:id 2, :bets [3500], :stack 0, :status :player-status/all-in})},
              :min-raise   2500}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :opts       {:bb 200}
                        :in-game-player-ids [1 2 3]
                        :street-bet 1000
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :bets   [500],
                                                                 :stack  3000,
                                                                 :status :player-status/acted}),
                                     2 (model/make-player-state
                                        {:id     2,
                                         :bets   [500],
                                         :stack  3000,
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-raise {:player-id 2, :raise 3000}])
                 (select-keys [:players :next-events :min-raise])))))
  (t/testing "raise is too small"
    (t/is (thrown-with-msg? ExceptionInfo #"Raise is too small"
            (-> (engine/make-game-engine {} engine/default-game-opts)
                (assoc :opts       {:bb 200}
                       :min-raise  800
                       :in-game-player-ids [1 2 3]
                       :street-bet 1000
                       :status     :game-status/preflop
                       :players    {2 (model/make-player-state {:id     2,
                                                                :bet    500,
                                                                :stack  3000,
                                                                :status :player-status/in-action})})
                (process-event [:game-event/player-raise {:player-id 2, :raise 600}])))))
  (t/testing "can't raise because no one can call"
    (t/is (thrown-with-msg? ExceptionInfo #"Can't raise"
            (-> (engine/make-game-engine {} engine/default-game-opts)
                (assoc :opts       {:bb 200}
                       :min-raise  800
                       :in-game-player-ids [1 2 3]
                       :street-bet 1000
                       :status     :game-status/preflop
                       :players    {2 (model/make-player-state {:id     2,
                                                                :bet    500,
                                                                :stack  3000,
                                                                :status :player-status/in-action})})
                (process-event [:game-event/player-raise {:player-id 2, :raise 1800}]))))))

(t/deftest player-check
  (t/testing "success"
    (t/is (= {:next-events [[:game-event/next-player-or-stage {:player-id 2}]],
              :players     {2 (model/make-player-state
                               {:id 2, :bets [500], :stack 3000, :status :player-status/acted})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :opts       {:bb 200}
                        :in-game-player-ids [1 2 3]
                        :street-bet 500
                        :status     :game-status/preflop
                        :players    {2 (model/make-player-state
                                        {:id     2,
                                         :bets   [500],
                                         :stack  3000,
                                         :status :player-status/in-action})})
                 (process-event [:game-event/player-check {:player-id 2}])
                 (select-keys [:players :next-events]))))))

(t/deftest next-player-or-stage
  (t/testing "ask next player for action"
    (t/is (= {:players         {1 (model/make-player-state {:id     1,
                                                            :stack  200,
                                                            :bets   [100],
                                                            :status :player-status/in-action}),
                                2 (model/make-player-state
                                   {:id 2, :stack 200, :bets [400], :status :player-status/acted}),
                                3 (model/make-player-state
                                   {:id 3, :stack 200, :bets [400], :status :player-status/acted})},
              :status          :game-status/preflop,
              :schedule-events [(model/make-schedule-event {:timeout 30000,
                                                            :event   [:game-event/player-fold
                                                                      {:player-id
                                                                       1}]})]}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 400
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :stack  200,
                                                                 :bets   [100],
                                                                 :status :player-status/acted}),
                                     2 (model/make-player-state {:id     2,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/acted}),
                                     3 (model/make-player-state {:id     3,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/acted})})
                 (process-event [:game-event/next-player-or-stage {:player-id 3}])
                 (select-keys [:players :status :schedule-events])))))
  (t/testing "ask next player for action"
    (t/is (= {:next-events     [[:game-event/last-player {:player-id 2}]],
              :schedule-events nil}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 400
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :stack  200,
                                                                 :bets   [100],
                                                                 :status :player-status/fold}),
                                     2 (model/make-player-state
                                        {:id     2,
                                         :stack  200,
                                         :bets   [200],
                                         :status :player-status/wait-for-action}),
                                     3 (model/make-player-state {:id     3,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/fold})})
                 (process-event [:game-event/next-player-or-stage {:player-id 3}])
                 (select-keys [:next-events :schedule-events])))))
  (t/testing "all player all-in"
    (t/is (= {:next-events [[:game-event/count-pot {:collapse-all? true}]
                            [:game-event/runner-prepare {}]]}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 400
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :stack  200,
                                                                 :bets   [100],
                                                                 :status :player-status/fold}),
                                     2 (model/make-player-state {:id     2,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/all-in}),
                                     3 (model/make-player-state {:id     3,
                                                                 :stack  200,
                                                                 :bets   [600],
                                                                 :status :player-status/all-in})})
                 (process-event [:game-event/next-player-or-stage {:player-id 2}])
                 (select-keys [:next-events])))))
  (t/testing "next street"
    (t/is (= {:next-events [[:game-event/flop-street {:action-player-id 1}]
                            [:game-event/count-pot {}]]}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 400
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/acted}),
                                     2 (model/make-player-state {:id     2,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/acted}),
                                     3 (model/make-player-state {:id     3,
                                                                 :stack  200,
                                                                 :bets   [200],
                                                                 :status :player-status/fold})})
                 (process-event [:game-event/next-player-or-stage {:player-id 3}])
                 (select-keys [:next-events])))))
  (t/testing "next-street"
    (t/is (= {:next-events [[:game-event/flop-street {:action-player-id 2}]
                            [:game-event/count-pot {}]],
              :players
              {1 (model/make-player-state
                  {:id 1, :stack 200, :bets [100], :status :player-status/fold}),
               2 (model/make-player-state
                  {:id 2, :stack 200, :bets [400], :status :player-status/acted}),
               3 (model/make-player-state
                  {:id 3, :stack 200, :bets [400], :status :player-status/acted})}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :in-game-player-ids [1 2 3]
                        :street-bet 400
                        :status     :game-status/preflop
                        :players    {1 (model/make-player-state {:id     1,
                                                                 :stack  200,
                                                                 :bets   [100],
                                                                 :status :player-status/fold}),
                                     2 (model/make-player-state {:id     2,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/acted}),
                                     3 (model/make-player-state {:id     3,
                                                                 :stack  200,
                                                                 :bets   [400],
                                                                 :status :player-status/acted})})
                 (process-event [:game-event/next-player-or-stage])
                 (select-keys [:players :next-events]))))))

(t/deftest flop-street
  (t/testing "success"
    (let [[c1 c2 c3 & cs :as cards] (misc/create-deck-of-cards)]
      (t/is (= {:community-cards [c1 c2 c3],
                :players         {1 (model/make-player-state {:id     1,
                                                              :bets   [100 nil],
                                                              :stack  200,
                                                              :status :player-status/fold}),
                                  2 (model/make-player-state {:id     2,
                                                              :bets   [400 nil],
                                                              :stack  200,
                                                              :status :player-status/in-action}),
                                  3 (model/make-player-state
                                     {:id     3,
                                      :bets   [400 nil],
                                      :stack  200,
                                      :status :player-status/wait-for-action})},
                :cards           cs,
                :status          :game-status/flop,
                :next-events     []}
               (-> (engine/make-game-engine {} engine/default-game-opts)
                   (assoc :in-game-player-ids [1 2 3]
                          :street-bet 400
                          :cards      cards
                          :status     :game-status/preflop
                          :players    {1 (model/make-player-state {:id     1,
                                                                   :stack  200,
                                                                   :bets   [100],
                                                                   :status :player-status/fold}),
                                       2 (model/make-player-state {:id     2,
                                                                   :stack  200,
                                                                   :bets   [400],
                                                                   :status :player-status/acted}),
                                       3 (model/make-player-state {:id     3,
                                                                   :stack  200,
                                                                   :bets   [400],
                                                                   :status :player-status/acted})})
                   (process-event [:game-event/flop-street {:action-player-id 2}])
                   (select-keys [:next-events :cards :players :community-cards :status])))))))

(t/deftest turn-street
  (t/testing "success"
    (let [[c & cs :as cards] (misc/create-deck-of-cards)]
      (t/is (= {:community-cards [[:s :a] [:s :k] [:s :q] c],
                :players         {1 (model/make-player-state {:id     1,
                                                              :stack  200,
                                                              :bets   [100 nil],
                                                              :status :player-status/fold}),
                                  2 (model/make-player-state {:id     2,
                                                              :stack  200,
                                                              :bets   [400 nil],
                                                              :status :player-status/in-action}),
                                  3 (model/make-player-state
                                     {:id     3,
                                      :stack  200,
                                      :bets   [400 nil],
                                      :status :player-status/wait-for-action})},
                :cards           cs,
                :status          :game-status/turn,
                :next-events     []}
               (-> (engine/make-game-engine {} engine/default-game-opts)
                   (assoc :community-cards [[:s :a] [:s :k] [:s :q]]
                          :in-game-player-ids [1 2 3]
                          :street-bet      400
                          :cards           cards
                          :status          :game-status/flop
                          :players         {1 (model/make-player-state
                                               {:id     1,
                                                :stack  200,
                                                :bets   [100],
                                                :status :player-status/fold}),
                                            2 (model/make-player-state
                                               {:id     2,
                                                :stack  200,
                                                :bets   [400],
                                                :status :player-status/acted}),
                                            3 (model/make-player-state
                                               {:id     3,
                                                :stack  200,
                                                :bets   [400],
                                                :status :player-status/acted})})
                   (process-event [:game-event/turn-street {:action-player-id 2}])
                   (select-keys [:next-events :cards :players :community-cards :status])))))))

(t/deftest river-street
  (t/testing "success"
    (let [[c & cs :as cards] (misc/create-deck-of-cards)]
      (t/is (= {:community-cards [[:s :a] [:s :k] [:s :q] [:s :t] c],
                :players         {1 (model/make-player-state {:id     1,
                                                              :stack  200,
                                                              :bets   [100 nil],
                                                              :status :player-status/fold}),
                                  2 (model/make-player-state {:id     2,
                                                              :stack  200,
                                                              :bets   [400 nil],
                                                              :status :player-status/in-action}),
                                  3 (model/make-player-state
                                     {:id     3,
                                      :stack  200,
                                      :bets   [400 nil],
                                      :status :player-status/wait-for-action})},
                :cards           cs,
                :status          :game-status/river,
                :next-events     []}
               (-> (engine/make-game-engine {} engine/default-game-opts)
                   (assoc :community-cards [[:s :a] [:s :k] [:s :q] [:s :t]]
                          :in-game-player-ids [1 2 3]
                          :street-bet      400
                          :cards           cards
                          :status          :game-status/turn
                          :players         {1 (model/make-player-state
                                               {:id     1,
                                                :stack  200,
                                                :bets   [100],
                                                :status :player-status/fold}),
                                            2 (model/make-player-state
                                               {:id     2,
                                                :stack  200,
                                                :bets   [400],
                                                :status :player-status/acted}),
                                            3 (model/make-player-state
                                               {:id     3,
                                                :stack  200,
                                                :bets   [400],
                                                :status :player-status/acted})})
                   (process-event [:game-event/river-street {:action-player-id 2}])
                   (select-keys [:next-events :cards :players :community-cards :status])))))))

(t/deftest last-player
  (t/testing "success"
    ;; most of state properties should be reset here
    (t/is (= {:schedule-events    [(model/make-schedule-event {:timeout 4000,
                                                               :event   [:game-event/next-game
                                                                         {}]})],
              :status             :game-status/last-player-settlement,
              :in-game-player-ids [],
              :cards              nil,
              :pots               [{:player-ids #{2}, :value 800, :winner-ids [2]}],
              :street-bet         nil,
              :community-cards    [],
              :min-raise          nil,
              :showdown           nil,
              :players            {1 (model/make-player-state
                                      {:id     1,
                                       :stack  200,
                                       :bets   [200],
                                       :status :player-status/fold}),
                                   2 (model/make-player-state
                                      {:id     2,
                                       ;; we check the stack award
                                       :stack  1000,
                                       :bets   [400],
                                       :status :player-status/acted}),
                                   3 (model/make-player-state
                                      {:id     3,
                                       :stack  200,
                                       :bets   [200],
                                       :status :player-status/fold})},
              :awards             {2 800}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :community-cards [[:s :a] [:s :k] [:s :q] [:s :t]]
                        :pots            [{:player-ids #{2}, :value 800}]
                        :min-raise       200
                        :in-game-player-ids [1 2 3]
                        :street-bet      400
                        :status          :game-status/turn
                        :players         {1 (model/make-player-state {:id     1,
                                                                      :stack  200,
                                                                      :bets   [200],
                                                                      :status :player-status/fold}),
                                          2 (model/make-player-state
                                             {:id     2,
                                              :stack  200,
                                              :bets   [400],
                                              :status :player-status/acted}),
                                          3 (model/make-player-state
                                             {:id     3,
                                              :stack  200,
                                              :bets   [200],
                                              :status :player-status/fold})})
                 (process-event [:game-event/last-player {:player-id 2}])
                 (select-keys [:status
                               :in-game-player-ids
                               :cards
                               :pots
                               :street-bet
                               :community-cards
                               :min-raise
                               :showdown
                               :players
                               :awards
                               :winners
                               :schedule-events]))))))

(t/deftest showdown
  (t/testing "single pot"
    (t/is
     (= {:schedule-events    [(model/make-schedule-event {:timeout 10000,
                                                          :event   [:game-event/next-game {}]})],
         :status             :game-status/showdown-settlement,
         :in-game-player-ids [],
         :cards              nil,
         :pots               [{:player-ids #{1 2 3}, :value 2400, :winner-ids [1]}],
         :street-bet         nil,
         :community-cards    [[:s :a] [:s :k] [:s :q] [:s :t] [:h :7]],
         :min-raise          nil,
         :showdown           {1 {:hole-cards [[:s :j] [:s :2]],
                                 :category   :royal-flush,
                                 :picks      [[:s :a] [:s :k] [:s :q] [:s :j] [:s :t]],
                                 :value      [9 14 13 12 11 10]},
                              2 {:hole-cards [[:c :6] [:s :6]],
                                 :category   :flush,
                                 :picks      [[:s :a] [:s :k] [:s :q] [:s :t] [:s :6]],
                                 :value      [5 14 13 12 10 6]},
                              3 {:hole-cards [[:d :6] [:h :6]],
                                 :category   :pair,
                                 :picks      [[:d :6] [:h :6] [:s :a] [:s :k] [:s :q]],
                                 :value      [1 6 6 14 13 12]}},
         :players            {1 (model/make-player-state
                                 {:id         1,
                                  :stack      3400,
                                  :status     :player-status/acted,
                                  :bets       [500 300 nil],
                                  :hole-cards [[:s :j] [:s :2]]}),
                              2 (model/make-player-state
                                 {:id         2,
                                  :stack      1000,
                                  :status     :player-status/acted,
                                  :bets       [500 300 nil],
                                  :hole-cards [[:c :6] [:s :6]]}),
                              3 (model/make-player-state
                                 {:id         3,
                                  :stack      1000,
                                  :status     :player-status/acted,
                                  :bets       [500 300 nil],
                                  :hole-cards [[:d :6] [:h :6]]})},
         :awards             {1 2400}}
        (-> (engine/make-game-engine {} engine/default-game-opts)
            (assoc :community-cards [[:s :a] [:s :k] [:s :q] [:s :t] [:h :7]]
                   :pots            [{:player-ids #{1 2 3}, :value 2400}]
                   :min-raise       300
                   :in-game-player-ids [1 2 3]
                   :street-bet      nil
                   :status          :game-status/river
                   :players         {1 (model/make-player-state
                                        {:id         1,
                                         :stack      1000,
                                         :bets       [500 300 nil],
                                         :hole-cards [[:s :j] [:s :2]],
                                         :status     :player-status/acted}),
                                     2 (model/make-player-state
                                        {:id         2,
                                         :stack      1000,
                                         :hole-cards [[:c :6] [:s :6]],
                                         :bets       [500 300 nil],
                                         :status     :player-status/acted}),
                                     3 (model/make-player-state
                                        {:id         3,
                                         :stack      1000,
                                         :hole-cards [[:d :6] [:h :6]],
                                         :bets       [500 300 nil],
                                         :status     :player-status/acted})})
            (process-event [:game-event/showdown {}])
            (select-keys [:status
                          :in-game-player-ids
                          :cards
                          :pots
                          :street-bet
                          :community-cards
                          :min-raise
                          :showdown
                          :players
                          :awards
                          :schedule-events])))))

  (t/testing "multiple pots"
    (t/is (= {:status             :game-status/showdown-settlement,
              :in-game-player-ids [],
              :cards              nil,
              :pots               [{:player-ids #{1 2}, :value 4600, :winner-ids [1]}],
              :street-bet         nil,
              :community-cards    [[:s :a] [:s :k] [:s :q] [:s :t] [:h :7]],
              :min-raise          nil,
              :showdown           {1 {:hole-cards [[:s :j] [:s :2]],
                                      :category   :royal-flush,
                                      :picks      [[:s :a] [:s :k] [:s :q] [:s :j] [:s :t]],
                                      :value      [9 14 13 12 11 10]},
                                   2 {:hole-cards [[:c :6] [:s :6]],
                                      :category   :flush,
                                      :picks      [[:s :a] [:s :k] [:s :q] [:s :t] [:s :6]],
                                      :value      [5 14 13 12 10 6]}},
              :players            {1 (model/make-player-state
                                      {:id         1,
                                       :stack      6100,
                                       :status     :player-status/all-in,
                                       :bets       [500 300 3000],
                                       :hole-cards [[:s :j] [:s :2]]}),
                                   2 (model/make-player-state
                                      {:id         2,
                                       :stack      0,
                                       :status     :player-status/acted,
                                       :bets       [500 300 1500],
                                       :hole-cards [[:c :6] [:s :6]]}),
                                   3 (model/make-player-state
                                      {:id         3,
                                       :stack      1000,
                                       :status     :player-status/fold,
                                       :bets       [500 300 nil],
                                       :hole-cards [[:d :6] [:h :6]]})},
              :awards             {1 4600}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :community-cards [[:s :a] [:s :k] [:s :q] [:s :t] [:h :7]]
                        :pots            [{:player-ids #{1 2}, :value 4600}
                                          {:player-ids #{1}, :value 1500}]
                        :min-raise       300
                        :in-game-player-ids [1 2 3]
                        :street-bet      nil
                        :status          :game-status/river
                        :players         {1 (model/make-player-state
                                             {:id         1,
                                              :stack      0,
                                              :bets       [500 300 3000],
                                              :hole-cards [[:s :j] [:s :2]],
                                              :status     :player-status/all-in}),
                                          2 (model/make-player-state
                                             {:id         2,
                                              :stack      0,
                                              :hole-cards [[:c :6] [:s :6]],
                                              :bets       [500 300 1500],
                                              :status     :player-status/acted}),
                                          3 (model/make-player-state
                                             {:id         3,
                                              :stack      1000,
                                              :hole-cards [[:d :6] [:h :6]],
                                              :bets       [500 300 nil],
                                              :status     :player-status/fold})})
                 (process-event [:game-event/showdown {}])
                 (select-keys [:status
                               :in-game-player-ids
                               :cards
                               :pots
                               :street-bet
                               :community-cards
                               :min-raise
                               :showdown
                               :players
                               :awards])))))
  (t/testing "tie game"
    (t/is (= {:status             :game-status/showdown-settlement,
              :in-game-player-ids [],
              :cards              nil,
              :pots               [{:player-ids #{1 2}, :value 4600, :winner-ids [1 2]}],
              :street-bet         nil,
              :community-cards    [[:s :a] [:c :a] [:h :2] [:s :t] [:h :7]],
              :min-raise          nil,
              :showdown           {1 {:hole-cards [[:d :a] [:d :3]],
                                      :category   :three-of-a-kind,
                                      :picks      [[:d :a] [:s :a] [:c :a] [:s :t] [:h :7]],
                                      :value      [3 14 14 14 10 7]},
                                   2 {:hole-cards [[:h :a] [:s :6]],
                                      :category   :three-of-a-kind,
                                      :picks      [[:h :a] [:s :a] [:c :a] [:s :t] [:h :7]],
                                      :value      [3 14 14 14 10 7]}},
              :players            {1 (model/make-player-state
                                      {:id         1,
                                       :stack      3800,
                                       :status     :player-status/all-in,
                                       :bets       [500 300 3000],
                                       :hole-cards [[:d :a] [:d :3]]}),
                                   2 (model/make-player-state
                                      {:id         2,
                                       :stack      2300,
                                       :status     :player-status/acted,
                                       :bets       [500 300 1500],
                                       :hole-cards [[:h :a] [:s :6]]}),
                                   3 (model/make-player-state
                                      {:id         3,
                                       :stack      1000,
                                       :status     :player-status/fold,
                                       :bets       [500 300 nil],
                                       :hole-cards [[:d :6] [:h :6]]})},
              :awards             {2 2300, 1 2300}}
             (-> (engine/make-game-engine {} engine/default-game-opts)
                 (assoc :community-cards [[:s :a] [:c :a] [:h :2] [:s :t] [:h :7]]
                        :pots            [{:player-ids #{1 2}, :value 4600}
                                          {:player-ids #{1}, :value 1500}]
                        :min-raise       300
                        :in-game-player-ids [1 2 3]
                        :street-bet      nil
                        :status          :game-status/river
                        :players         {1 (model/make-player-state
                                             {:id         1,
                                              :stack      0,
                                              :bets       [500 300 3000],
                                              :hole-cards [[:d :a] [:d :3]],
                                              :status     :player-status/all-in}),
                                          2 (model/make-player-state
                                             {:id         2,
                                              :stack      0,
                                              :hole-cards [[:h :a] [:s :6]],
                                              :bets       [500 300 1500],
                                              :status     :player-status/acted}),
                                          3 (model/make-player-state
                                             {:id         3,
                                              :stack      1000,
                                              :hole-cards [[:d :6] [:h :6]],
                                              :bets       [500 300 nil],
                                              :status     :player-status/fold})})
                 (process-event [:game-event/showdown {}])
                 (select-keys [:status
                               :in-game-player-ids
                               :cards
                               :pots
                               :street-bet
                               :community-cards
                               :min-raise
                               :showdown
                               :players
                               :awards]))))))

(t/deftest runner-prepare
  (t/is (= {:schedule-events [(model/make-schedule-event {:timeout 4000,
                                                          :event   [:game-event/runner-run {}]})],
            :pots            [{:player-ids #{1 2 3}, :value 3900} {:player-ids #{1 3}, :value 500}],
            :players         {1 (model/make-player-state {:id     1,
                                                          :stack  0,
                                                          :bets   [500 300 2000],
                                                          :status :player-status/all-in}),
                              2 (model/make-player-state {:id     2,
                                                          :stack  0,
                                                          :bets   [500 300 1500],
                                                          :status :player-status/all-in}),
                              3 (model/make-player-state {:id     3,
                                                          :stack  3000,
                                                          :bets   [500 300 3000],
                                                          :status :player-status/all-in})}}
           (-> (engine/make-game-engine {} engine/default-game-opts)
               (assoc :pots    [{:player-ids #{1 2 3}, :value 3900}
                                {:player-ids #{1 3}, :value 500}
                                {:player-ids #{3}, :value 1000}]
                      :in-game-player-ids [1 2 3]
                      :status  :game-status/river
                      :players {1 (model/make-player-state {:id     1,
                                                            :stack  0,
                                                            :bets   [500 300 2000],
                                                            :status :player-status/all-in}),
                                2 (model/make-player-state {:id     2,
                                                            :stack  0,
                                                            :bets   [500 300 1500],
                                                            :status :player-status/all-in}),
                                3 (model/make-player-state {:id     3,
                                                            :stack  2000,
                                                            :bets   [500 300 3000],
                                                            :status :player-status/all-in})})
               (process-event [:game-event/runner-prepare {}])
               (select-keys [:pots :players :schedule-events])))))

(t/deftest player-request-deal-times
  (t/is
   (thrown-with-msg? ExceptionInfo #"Invalid times"
     (-> (engine/make-game-engine {} engine/default-game-opts)
         (assoc :status  :game-status/runner-prepare
                :players {1 (model/make-player-state {:id     1,
                                                      :stack  0,
                                                      :bets   [500 300 2000],
                                                      :status :player-status/all-in}),
                          2 (model/make-player-state {:id     2,
                                                      :stack  0,
                                                      :bets   [500 300 1500],
                                                      :status :player-status/all-in}),
                          3 (model/make-player-state {:id     3,
                                                      :stack  3000,
                                                      :bets   [500 300 3000],
                                                      :status :player-status/all-in})}
                :runner  (model/make-runner {}))
         (process-event [:game-event/player-request-deal-times {:player-id 1, :deal-times 4}]))))
  (t/testing "success"
    (t/is
     (thrown-with-msg? ExceptionInfo #"Invalid times"
       (-> (engine/make-game-engine {} engine/default-game-opts)
           (assoc :status  :game-status/runner-prepare
                  :players {1 (model/make-player-state {:id     1,
                                                        :stack  0,
                                                        :bets   [500 300 2000],
                                                        :status :player-status/all-in}),
                            2 (model/make-player-state {:id     2,
                                                        :stack  0,
                                                        :bets   [500 300 1500],
                                                        :status :player-status/all-in}),
                            3 (model/make-player-state {:id     3,
                                                        :stack  3000,
                                                        :bets   [500 300 3000],
                                                        :status :player-status/all-in})}
                  :runner  (model/make-runner {}))
           (process-event [:game-event/player-request-deal-times {:player-id 1, :deal-times 2}])
           (process-event [:game-event/player-request-deal-times {:player-id 2, :deal-times 2}])
           (select-keys [:runner]))))))

(t/deftest runner-run
  (t/testing "success, run twice"
    (t/is
     (= {:awards  {3 4400},
         :players {1 (model/make-player-state
                      {:id         1,
                       :stack      0,
                       :status     :player-status/all-in,
                       :bets       [500 300 2000],
                       :hole-cards [[:s :6] [:s :2]],
                       :seat       1}),
                   2 (model/make-player-state
                      {:id         2,
                       :stack      0,
                       :status     :player-status/all-in,
                       :bets       [500 300 1500],
                       :hole-cards [[:h :8] [:c :8]],
                       :seat       2}),
                   3 (model/make-player-state
                      {:id         3,
                       :stack      8400,
                       :status     :player-status/all-in,
                       :bets       [500 300 3000],
                       :hole-cards [[:d :8] [:d :a]],
                       :seat       3})},
         :runner  (model/make-runner
                   {:player-deal-times {1 2},
                    :deal-times        2,
                    :results           [{:community-cards [[:s :a] [:s :q] [:h :t] [:h :7] [:h :a]],
                                         :pots            [(model/make-pot {:player-ids #{1 2 3},
                                                                            :value      1950,
                                                                            :winner-ids [3],
                                                                            :street     :turn})
                                                           (model/make-pot {:player-ids #{1 3},
                                                                            :value      250,
                                                                            :winner-ids [3],
                                                                            :street     :turn})],
                                         :awards          {3 2200},
                                         :showdown        {1 {:category   :pair,
                                                              :picks      [[:s :a]
                                                                           [:h :a]
                                                                           [:s :q]
                                                                           [:h :t]
                                                                           [:h :7]],
                                                              :value      [1 14 14 12 10 7],
                                                              :hole-cards [[:s :6] [:s :2]]},
                                                           2 {:category   :two-pairs,
                                                              :picks      [[:s :a]
                                                                           [:h :a]
                                                                           [:h :8]
                                                                           [:c :8]
                                                                           [:s :q]],
                                                              :value      [2 14 14 8 8 12],
                                                              :hole-cards [[:h :8] [:c :8]]},
                                                           3 {:category   :three-of-a-kind,
                                                              :picks      [[:d :a]
                                                                           [:s :a]
                                                                           [:h :a]
                                                                           [:s :q]
                                                                           [:h :t]],
                                                              :value      [3 14 14 14 12 10],
                                                              :hole-cards [[:d :8] [:d :a]]}}}
                                        {:community-cards [[:s :a] [:s :q] [:h :t] [:c :k] [:c :5]],
                                         :pots            [(model/make-pot {:player-ids #{1 2 3},
                                                                            :value      1950,
                                                                            :winner-ids [3],
                                                                            :street     :turn})
                                                           (model/make-pot {:player-ids #{1 3},
                                                                            :value      250,
                                                                            :winner-ids [3],
                                                                            :street     :turn})],
                                         :awards          {3 2200},
                                         :showdown        {1 {:category   :highcard,
                                                              :picks      [[:s :a]
                                                                           [:c :k]
                                                                           [:s :q]
                                                                           [:h :t]
                                                                           [:s :6]],
                                                              :value      [0 14 13 12 10 6],
                                                              :hole-cards [[:s :6] [:s :2]]},
                                                           2 {:category   :pair,
                                                              :picks      [[:h :8]
                                                                           [:c :8]
                                                                           [:s :a]
                                                                           [:c :k]
                                                                           [:s :q]],
                                                              :value      [1 8 8 14 13 12],
                                                              :hole-cards [[:h :8] [:c :8]]},
                                                           3 {:category   :pair,
                                                              :picks      [[:d :a]
                                                                           [:s :a]
                                                                           [:c :k]
                                                                           [:s :q]
                                                                           [:h :t]],
                                                              :value      [1 14 14 13 12 10],
                                                              :hole-cards [[:d :8] [:d :a]]}}}]})}
        (-> (engine/make-game-engine {} engine/default-game-opts)
            (assoc :status          :game-status/runner-prepare
                   :community-cards [[:s :a] [:s :q] [:h :t]]
                   :cards           [[:h :7] [:h :a] [:c :k] [:c :5]]
                   :pots            [(model/make-pot {:player-ids #{1 2 3},
                                                      :value      3900,
                                                      :street     :turn})
                                     (model/make-pot {:player-ids #{1 3},
                                                      :value      500,
                                                      :street     :turn})]
                   :players         {1 (model/make-player-state {:id 1,
                                                                 :stack 0,
                                                                 :bets [500 300 2000],
                                                                 :seat 1,
                                                                 :hole-cards [[:s :6] [:s :2]],
                                                                 :status :player-status/all-in}),
                                     2 (model/make-player-state {:id 2,
                                                                 :stack 0,
                                                                 :seat 2,
                                                                 :hole-cards [[:h :8] [:c :8]],
                                                                 :bets [500 300 1500],
                                                                 :status :player-status/all-in}),
                                     3 (model/make-player-state {:id 3,
                                                                 :seat 3,
                                                                 :stack 4000,
                                                                 :hole-cards [[:d :8] [:d :a]],
                                                                 :bets [500 300 3000],
                                                                 :status :player-status/all-in})}
                   :runner          (model/make-runner {:deal-times 2, :player-deal-times {1 2}}))
            (process-event [:game-event/runner-run {}])
            (select-keys [:runner :awards :players]))))))

(t/deftest next-game
  (t/testing "success, remove dropout & off-seat players"
    (t/is
     (= {:ladder-events [[:ladder-event/player-returns
                          {:player-id 4,
                           :returns   4000}]
                         [:ladder-event/player-returns
                          {:player-id 3,
                           :returns   0}]
                         [:ladder-event/player-inc-hand
                          {:player-id 2}]
                         [:ladder-event/player-inc-hand
                          {:player-id 1}]],
         :seats         {2 2},
         :players       {1 (model/make-player-state {:id         1,
                                                     :stack      0,
                                                     :bets       nil,
                                                     :seat       nil,
                                                     :hole-cards nil,
                                                     :status     :player-status/off-seat}),
                         2 (model/make-player-state {:id         2,
                                                     :stack      200,
                                                     :seat       2,
                                                     :hole-cards nil,
                                                     :prev-stack 200,
                                                     :bets       nil,
                                                     :status     :player-status/wait-for-start})}}
        (-> (engine/make-game-engine {} engine/default-game-opts)
            (assoc :status          :game-status/settlement
                   :community-cards [[:s :a] [:s :q] [:h :t]]
                   :seats           {1 1,
                                     2 2,
                                     3 3}
                   :players         {1 (model/make-player-state {:id 1,
                                                                 :stack 0,
                                                                 :bets [500 300 2000],
                                                                 :seat 1,
                                                                 :hole-cards [[:s :6] [:s :2]],
                                                                 :status :player-status/off-seat}),
                                     2 (model/make-player-state {:id 2,
                                                                 :stack 200,
                                                                 :seat 2,
                                                                 :hole-cards [[:h :8] [:c :8]],
                                                                 :bets [500 300 1500],
                                                                 :status :player-status/all-in}),
                                     3 (model/make-player-state
                                        {:id            3,
                                         :seat          3,
                                         :stack         0,
                                         :hole-cards    [[:d :8] [:d :a]],
                                         :bets          [500 300 3000],
                                         :status        :player-status/all-in,
                                         :network-error :network-error/dropout}),
                                     4 (model/make-player-state
                                        {:id         4,
                                         :seat       3,
                                         :stack      4000,
                                         :hole-cards [[:d :8] [:d :a]],
                                         :bets       [500 300 3000],
                                         :status     :player-status/leave})})
            (process-event [:game-event/next-game {}])
            (select-keys [:seats :players :ladder-events]))))))
