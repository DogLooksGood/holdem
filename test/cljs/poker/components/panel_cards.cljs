(ns poker.components.panel-cards
  (:require
   [nubank.workspaces.core :as ws]
   [nubank.workspaces.card-types.react :as ct.react]
   [poker.components.panel :as sut]
   [reagent.core :as reagent]))

(ws/defcard runner-button-group
  (ct.react/react-card
   (reagent/as-element
    [sut/runner-button-group {:pot-val     200,
                              :stack       2000,
                              :street-bet  100,
                              :buttons-set #{:raise :fold :call}}])))

(ws/defcard bet-button-group
  (ct.react/react-card
   (reagent/as-element
    [sut/bet-button-group {:pot-val     200,
                           :stack       2000,
                           :street-bet  100,
                           :buttons-set #{:raise :fold :call}}])))

(ws/defcard panel-when-runner-prepare
  (ct.react/react-card
   (reagent/as-element
    [sut/panel {:current-player {:id         1,
                                 :status     :player-status/all-in,
                                 :hole-cards [[:d :j] [:d :k]],
                                 :position   "BTN1",
                                 :name       "foo",
                                 :stack      200,
                                 :avatar     nil},
                :pots           [{:value      2000,
                                  :player-ids [1]}],
                :street-bet     400,
                :status         :game-status/runner-prepare}])))
