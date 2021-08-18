(ns poker.game.engine-test
  (:require
    [poker.game.engine    :as sut]
    [poker.game.protocols :as p]
    [clojure.test         :as t]))


(t/deftest next-event
  (t/testing "no next event"
    (t/is (= [nil
              (-> (sut/make-game-engine {} sut/default-game-opts))]
             (-> (sut/make-game-engine {} sut/default-game-opts) (p/next-event)))))
  (t/testing "has next event"
    (t/is (= [[:game-event/collect-players {}]
              (-> (sut/make-game-engine {} sut/default-game-opts)
                  (assoc :next-events [[:game-event/deal-cards {}]]))]
             (-> (sut/make-game-engine {} sut/default-game-opts)
                 (assoc :next-events [[:game-event/collect-players {}] [:game-event/deal-cards {}]])
                 (p/next-event))))))
