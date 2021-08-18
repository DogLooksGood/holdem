(ns poker.account-test
  (:require
   [poker.account :as sut]
   [clojure.test  :as t]
   [poker.utils.test-system :refer [wrap-test-system]])
  (:import clojure.lang.ExceptionInfo))

(t/use-fixtures :each wrap-test-system)

(t/deftest signup!
  (t/testing "success"
    (t/is (map? (sut/signup! {:player/name   "foo",
                              :player/avatar "AVATAR"})))
    (t/is
     (= {:player/name    "foo",
         :player/avatar  "AVATAR",
         :player/balance 10000}
        (sut/get-player [:player/name :player/avatar :player/balance]
                        {:player/name "foo"}))))
  (t/testing "disallow duplicate names"
    (t/is (thrown-with-msg? ExceptionInfo #"Player name not available"
            (sut/signup! {:player/name   "foo",
                          :player/avatar "AVATAR"})))))

(t/deftest auth-player-by-token!
  (let [{:player/keys [token]} (sut/signup! {:player/name "foo",
                                             :player/avatar
                                             "AVATAR"})]
    (t/testing "prepare token" (t/is (uuid? token)))
    (t/testing "success"
      (t/is (= {:player/name    "foo",
                :player/avatar  "AVATAR",
                :player/id      {:player/name "foo"},
                :crux.db/id     {:player/name "foo"},
                :player/token   token,
                :player/balance 10000}
               (sut/auth-player-by-token! '[*] token))))
    (t/testing "failed"
      (t/is
       (thrown-with-msg? ExceptionInfo #"Player token invalid"
         (sut/auth-player-by-token! [:*]
                                    (java.util.UUID/randomUUID)))))))
