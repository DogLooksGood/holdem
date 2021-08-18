(ns poker.components.table-cards
  (:require
   [poker.components.table :as sut]
   [nubank.workspaces.core :as ws]
   [nubank.workspaces.card-types.react :as ct.react]
   [nubank.workspaces.model :as wsm]
   [reagent.core :as reagent]))

(ws/defcard render-community-cards-3
  (ct.react/react-card
   (reagent/as-element
    [sut/render-community-cards [[:h :a] [:h :k] [:h :q]]])))

(ws/defcard render-community-cards-4
  (ct.react/react-card
   (reagent/as-element
    [sut/render-community-cards [[:h :a] [:h :k] [:h :q] [:h :j]]])))

(ws/defcard render-community-cards-5
  (ct.react/react-card
   (reagent/as-element
    [sut/render-community-cards [[:h :a] [:h :k] [:h :q] [:h :j] [:h :t]]])))

(ws/defcard render-community-cards-deal-all
  (ct.react/react-card
   (reagent/as-element
    [sut/render-community-cards-deal-all [[:h :a] [:h :k] [:h :q] [:h :j] [:h :t]]])))

(ws/defcard table-loading
  {::wsm/align {:flex 1, :display :flex}}
  (ct.react/react-card
   (reagent/as-element
    [sut/table {}])))

(ws/defcard table-off-seat
  {::wsm/align {:flex 1, :display :flex}}
  (ct.react/react-card
   (reagent/as-element
    [sut/table
     {:current-player {:status :player-status/off-seat},
      :players        [{:seat 1}
                       {:seat 2}
                       {:seat 3}
                       {:seat   4,
                        :status :player-status/wait-for-action,
                        :name   "bar",
                        :stack  10000}
                       {:seat 5}
                       {:seat 6}]}])))

(ws/defcard table-with-two-player
  {::wsm/align {:flex 1, :display :flex}}
  (ct.react/react-card
   (reagent/as-element
    [sut/table
     {:current-player {:seat   3,
                       :status :player-status/wait-for-action,
                       :name   "foo",
                       :stack  5000},
      :players        [{:seat   4,
                        :status :player-status/wait-for-action,
                        :name   "bar",
                        :stack  10000}
                       {:seat 5}
                       {:seat 6}
                       {:seat 1}
                       {:seat 2}]}])))

(ws/defcard table-with-six-player
  {::wsm/align {:flex 1, :display :flex}}
  (ct.react/react-card
   (reagent/as-element
    [sut/table
     {:runner-cards    [[[:s :a] [:s :k] [:s :q] [:s :j] [:s :t]]],
      :community-cards [[:s :a] [:s :k] [:s :q] [:s :j] [:s :t]],
      :status          :game-status/turn,
      :pots            [{:player-ids [3 4], :value 500, :street :preflop}
                        {:player-ids [3 4], :value 1024, :street :flop}],
      :current-player  {:seat     3,
                        :status   :player-status/wait-for-action,
                        :name     "hero",
                        :position "BB",
                        :bets     [50 100 3027],
                        :stack    800},
      :players         [{:seat     3,
                         :status   :player-status/wait-for-action,
                         :name     "hero",
                         :position "BB",
                         :bets     [50 100 3027],
                         :stack    800}
                        {:seat     4,
                         :status   :player-status/wait-for-action,
                         :name     "foo",
                         :stack    1000,
                         :position "BB",
                         :bets     [50 100 3027]}
                        {:seat     5,
                         :status   :player-status/fold,
                         :name     "bar",
                         :stack    800,
                         :position "BB",
                         :bets     [50 100 3027]}
                        {:seat     6,
                         :status   :player-status/all-in,
                         :name     "mike's name is long",
                         :stack    0,
                         :position "BB",
                         :bets     [50 100 3027]}
                        {:seat     1,
                         :status   :player-status/wait-for-action,
                         :name     "bill",
                         :stack    300,
                         :position "BB",
                         :bets     [50 100 3027]}
                        {:seat     1,
                         :status   :player-status/in-action,
                         :name     "andy",
                         :position "BB",
                         :stack    300,
                         :bets     [50 100 3027]}]}])))
