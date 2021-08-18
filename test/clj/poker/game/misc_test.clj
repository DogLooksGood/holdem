(ns poker.game.misc-test
  (:require
   [poker.game.misc  :as sut]
   [poker.game.model :as model]
   [clojure.test     :as t]))

(t/deftest init-player-status-for-new-street
  (t/testing "success"
    (t/is (= {1 (model/make-player-state {:id     1,
                                          :bets   [200 nil],
                                          :status :player-status/wait-for-action}),
              2 (model/make-player-state {:id 2, :bets [200 nil], :status :player-status/fold}),
              3 (model/make-player-state {:id     3,
                                          :bets   [200 nil],
                                          :status :player-status/in-action})}
             (sut/init-player-status-for-new-street
              {1 (model/make-player-state {:id 1, :bets [200], :status :player-status/acted}),
               2 (model/make-player-state {:id 2, :bets [200], :status :player-status/fold}),
               3 (model/make-player-state {:id 3, :bets [200], :status :player-status/acted})}
              3)))))


(t/deftest find-players-for-start
  (t/testing "two players"
    ;; Player 2 is the BTN
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-start})}
              1)))
    ;; Player 1 still be the BTN, since Player 2 is the new comer.
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-bb})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-bb})}
              1)))

    ;; Player 1 is the BTN, last btn-seat Player has left.
    (t/is (= [(model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     1,
                                        :seat   6,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   6,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-start})}
              3
             )))

    ;; Player 1 is the BTN, this is the case for a brand new game with two players
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-bb})
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-bb})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-bb}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-bb})}
              nil
             ))))

  (t/testing "three players"
    ;; Not enough players for switch BTN, start a new game with all three players
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-bb})
              (model/make-player-state {:id     3,
                                        :seat   3,
                                        :status :player-status/wait-for-bb})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-bb}),
               3 (model/make-player-state {:id     3,
                                           :seat   3,
                                           :status :player-status/wait-for-bb})}
              1)))

    ;; Player 2 is the BTN, Player 3 is the new comer need wait for BB
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-start}),
               3 (model/make-player-state {:id     3,
                                           :seat   3,
                                           :status :player-status/wait-for-bb})}
              1)))

    ;; Player 1 is the BTN, Player 3 join game
    (t/is (= [
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     3,
                                        :seat   3,
                                        :status :player-status/wait-for-bb})
              (model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-start}),
               3 (model/make-player-state {:id     3,
                                           :seat   3,
                                           :status :player-status/wait-for-bb})}
              2)))

    ;; Player 1 is the BTN, Player 2 is new comer & join game
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   1,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     2,
                                        :seat   2,
                                        :status :player-status/wait-for-bb})
              (model/make-player-state {:id     3,
                                        :seat   3,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   1,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   2,
                                           :status :player-status/wait-for-bb}),
               3 (model/make-player-state {:id     3,
                                           :seat   3,
                                           :status :player-status/wait-for-start})}
              1)))

    ;; Player 3 is the new comer, wait for BB
    (t/is (= [(model/make-player-state {:id     1,
                                        :seat   4,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     2,
                                        :seat   5,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   4,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   5,
                                           :status :player-status/wait-for-start}),
               3 (model/make-player-state {:id     3,
                                           :seat   2,
                                           :status :player-status/wait-for-bb})}
              4)))

    ;; Player 3 is the new comer, join game
    (t/is (= [(model/make-player-state {:id     2,
                                        :seat   5,
                                        :status :player-status/wait-for-start})
              (model/make-player-state {:id     3,
                                        :seat   2,
                                        :status :player-status/wait-for-bb})
              (model/make-player-state {:id     1,
                                        :seat   4,
                                        :status :player-status/wait-for-start})]
             (sut/find-players-for-start
              {1 (model/make-player-state {:id     1,
                                           :seat   4,
                                           :status :player-status/wait-for-start}),
               2 (model/make-player-state {:id     2,
                                           :seat   5,
                                           :status :player-status/wait-for-start}),
               3 (model/make-player-state {:id     3,
                                           :seat   2,
                                           :status :player-status/wait-for-bb})}
              5)))))


(t/deftest calc-showdown
  (t/testing "split value for multiple winners"
    (t/is (= {:pots     [(model/make-pot
                          {:player-ids #{1 2}, :value 603, :winner-ids [1 2], :street :river})],
              :awards   {2 302, 1 301},
              :showdown {1
                         {:category   :royal-flush,
                          :picks      [[:c :a] [:c :k] [:c :q] [:c :j] [:c :t]],
                          :value      [9 14 13 12 11 10],
                          :hole-cards [[:h :2] [:h :3]]},
                         2
                         {:category   :royal-flush,
                          :picks      [[:c :a] [:c :k] [:c :q] [:c :j] [:c :t]],
                          :value      [9 14 13 12 11 10],
                          :hole-cards [[:h :2] [:h :3]]}}}
             (sut/calc-showdown
              {:in-game-player-ids [2 1],
               :pots               [(model/make-pot {:player-ids #{1 2},
                                                     :value      603,
                                                     :street     :river})],
               :community-cards    [[:c :a] [:c :k] [:c :q] [:c :j] [:c :t]],
               :players            {1 (model/make-player-state
                                       {:id         1,
                                        :hole-cards [[:h :2] [:h :3]],
                                        :status     :player-status/acted,
                                        :bets       [200 nil nil 201]}),
                                    2 (model/make-player-state
                                       {:id         2,
                                        :hole-cards [[:h :2]
                                                     [:h :3]],
                                        :status     :player-status/acted,
                                        :bets       [200 nil nil 201]}),
                                    3 (model/make-player-state
                                       {:id         3,
                                        :hole-cards [[:s :2]
                                                     [:s :3]],
                                        :status     :player-status/fold,
                                        :bets       [200 nil nil 201]})}})))))

(t/deftest count-pot
  (t/testing "multi player pots"
    (t/is (= [(model/make-pot {:player-ids #{4 3 2 5}, :value 3200, :street :flop})
              (model/make-pot {:player-ids #{4 3 5}, :value 900, :street :turn})]
             (sut/count-pot :river
                            {1 {:id 1, :bets [200 200 nil], :status :player-status/fold},
                             2 {:id 2, :bets [200 500 nil], :status :player-status/all-in},
                             3 {:id 3, :bets [200 500 300 nil], :status :player-status/all-in},
                             4 {:id 4, :bets [200 500 300 nil], :status :player-status/acted},
                             5 {:id 5, :bets [200 500 300 nil], :status :player-status/acted}}))))

  (t/testing "pot collapse"
    (t/is (= [(model/make-pot {:player-ids #{1 2}, :value 1500, :street :preflop})
              (model/make-pot {:player-ids #{1 2}, :value 500, :street :flop})]
             (sut/count-pot :flop
                            {1 (model/make-player-state
                                {:id 1, :status :player-status/all-in, :bets [500 200], :stack 0}),
                             2 (model/make-player-state
                                {:id 2, :stack 0, :bets [500 200], :status :player-status/all-in}),
                             3 (model/make-player-state
                                {:id     3,
                                 :stack  9800,
                                 :bets   [500 100],
                                 :status :player-status/fold})})))
    (t/is (= [(model/make-pot {:player-ids #{1 2}, :value 2000, :street :flop})]
             (sut/count-pot nil ; <-- Provide nil `curr-street` will collapse all pots. The
                                ; case is for showdown or runner.
                            {1 (model/make-player-state
                                {:id 1, :status :player-status/all-in, :bets [500 200], :stack 0}),
                             2 (model/make-player-state
                                {:id 2, :stack 0, :bets [500 200], :status :player-status/all-in}),
                             3 (model/make-player-state
                                {:id     3,
                                 :stack  9800,
                                 :bets   [500 100],
                                 :status :player-status/fold})})))))
