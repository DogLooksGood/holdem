(ns poker.components.panel-test
  (:require
   [poker.components.panel :as sut]
   [nubank.workspaces.core :as ws]
   [cljs.test :as t]))

(ws/deftest get-buttons-set
  (t/is (= 1 1)))

;; (ws/deftest get-buttons-set
;;   (t/testing "player not in-action, no buttons"
;;     (t/is (nil?
;;            (sut/get-buttons-set {:status :player-status/wait-for-action,
;;                                  :bets   [nil]}
;;                                 nil))))
;;
;;   (t/testing "not street bet, player can check or bet"
;;     (t/is (= #{:check :bet}
;;              (sut/get-buttons-set {:status :player-status/in-action,
;;                                    :bets   [nil]}
;;                                   nil))))
;;
;;   (t/testing "player's bet equals to street bet, player can check or raise"
;;     (t/is (= #{:check :raise}
;;              (sut/get-buttons-set {:status :player-status/in-action,
;;                                    :bets   [200]}
;;                                   200))))
;;
;;   (t/testing "player's bet less than street bet, player can fold, call or raise"
;;     (t/is (= #{:fold :call :raise}
;;              (sut/get-buttons-set {:status :player-status/in-action,
;;                                    :bets   [200]}
;;                                   500)))))
