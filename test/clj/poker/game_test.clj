(ns poker.game-test
  (:require
   [poker.game         :as sut]
   [poker.game.model   :as model]
   [clojure.test       :as t]
   [poker.utils.test-system :refer [wrap-test-system]]
   [clojure.core.async :as a])
  (:import clojure.lang.ExceptionInfo))

(t/use-fixtures :each wrap-test-system)

(t/deftest start-game
  (t/testing "success, check all helpers work correctly"
    (let [id   (java.util.UUID/randomUUID)
          game (sut/start-game {:id id} {})]
      (t/is (= {:id        id,
                :game-opts sut/default-game-opts}
               (-> (sut/get-game id)
                   (select-keys [:id :game-opts]))))
      (t/is (some? (sut/get-game-input-ch game)))
      (t/is (some? (sut/get-game-input-ch game))))))

(t/deftest stop-game
  (t/testing "success"
    (let [id    (java.util.UUID/randomUUID)
          _game (sut/start-game {:id id} {})]
      (sut/stop-game id)
      (t/is (thrown-with-msg? ExceptionInfo #"Game not found"
              (sut/get-game id))))))

(t/deftest send-game-event
  (t/testing "success"
    (let [id   (java.util.UUID/randomUUID)
          game (sut/start-game {:id id} {})]
      (sut/send-game-event game
                           [:game-event/player-join {:id 1, :name "foo"}])
      (let [out (sut/get-game-output-ch game)]
        (t/is (= {:players {1 (model/make-player-state {:id 1, :name "foo"})}}
                 (-> (a/<!! out)
                     (second)
                     (select-keys [:players]))))))))
