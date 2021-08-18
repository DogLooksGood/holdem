(ns poker.lobby-test
  (:require
   [poker.lobby   :as sut]
   [poker.account :as account]
   [clojure.test  :as t]
   [poker.utils.test-system :refer [wrap-test-system]]
   [poker.game    :as game])
  (:import clojure.lang.ExceptionInfo))

(t/use-fixtures :each wrap-test-system)

(t/deftest create-game!
  (t/testing "success"
    (let [{:player/keys [_token]} (account/signup! {:player/name "foo", :player/avatar "AVATAR"})
          {:game/keys [id]}       (sut/create-game! {:game/title "game title",
                                                     :player/id  {:player/name "foo"}})]
      (t/is (uuid? id))
      (t/is (= {:game/title   "game title",
                :game/players #{{:player/name "foo"}},
                :game/opts    {},
                :game/creator {:player/name "foo"},
                :game/id      id,
                :game/status  :game-status/open,
                :crux.db/id   id}
               (sut/get-game id))))))

(t/deftest join-game!
  (t/testing "success"
    (let [{_token-1 :player/token} (account/signup! {:player/name "foo", :player/avatar "AVATAR"})
          {_token-2 :player/token} (account/signup! {:player/name "bar", :player/avatar "AVATAR"})
          {:game/keys [id]}        (sut/create-game! {:game/title "game title",
                                                      :player/id  {:player/name "foo"}})]
      (t/is (some? (:game/id
                    (sut/join-game! {:player/id {:player/name "bar"}, :game/id id}))))
      (t/is (= {:game/title   "game title",
                :game/players #{{:player/name "foo"} {:player/name "bar"}},
                :game/opts    {},
                :game/creator {:player/name "foo"},
                :game/status  :game-status/open,
                :game/id      id,
                :crux.db/id   id}
               (sut/get-game id))))))

(t/deftest integration-test
  (t/testing "success"
    (let [{_token-1 :player/token} (account/signup! {:player/name "foo", :player/avatar "AVATAR"})
          {_token-2 :player/token} (account/signup! {:player/name "bar", :player/avatar "AVATAR"})
          {:game/keys [id]}        (sut/create-game! {:game/title "game title",
                                                      :player/id  {:player/name "foo"}})]
      (t/is (map? (game/get-game id)))
      (t/is (some? (:game/id
                    (sut/join-game! {:player/id {:player/name "bar"}, :game/id id}))))
      (t/is (= {:game/title   "game title",
                :game/players #{{:player/name "foo"} {:player/name "bar"}},
                :game/opts    {},
                :game/creator {:player/name "foo"},
                :game/status  :game-status/open,
                :game/id      id,
                :crux.db/id   id}
               (sut/get-game id)))
      (t/is (= :ok (sut/leave-game! {:player/id {:player/name "foo"}, :game/id id})))
      (t/is (= {:game/title   "game title",
                :game/players #{{:player/name "bar"}},
                :game/opts    {},
                :game/creator {:player/name "foo"},
                :game/status  :game-status/open,
                :game/id      id,
                :crux.db/id   id}
               (sut/get-game id)))
      (t/is (= :ok (sut/leave-game! {:player/id {:player/name "bar"}, :game/id id})))
      (t/is (= {}
               (-> (sut/get-game id)
                   (select-keys [:game/players :game/status]))))
      (t/is (thrown-with-msg? ExceptionInfo #"Game not found"
              (game/get-game id))))))
