(ns poker.pages.game-test
  (:require
   [poker.pages.game :as sut]
   [nubank.workspaces.core :as ws]
   [cljs.test        :as t]))

(ws/deftest sort-players-for-display
  (t/is (= [{:id 2, :seat 2}
            {:id 4, :seat 3}
            {:id 5, :seat 4}
            {:id 1, :seat 5}
            {:seat 6}
            {:id 3, :seat 1}]
           (sut/sort-players-for-display {1 {:id 1, :seat 5},
                                          2 {:id 2, :seat 2},
                                          3 {:id 3, :seat 1},
                                          4 {:id 4, :seat 3},
                                          5 {:id 5, :seat 4}}
                                         2
                                         6)))
  (t/is (= [{:id 2, :seat 2} {:id 4, :seat 3} {:seat 4} {:id 1, :seat 5} {:seat 6} {:id 3, :seat 1}]
           (sut/sort-players-for-display
            {1 {:id 1, :seat 5}, 2 {:id 2, :seat 2}, 3 {:id 3, :seat 1}, 4 {:id 4, :seat 3}}
            2
            6))))
