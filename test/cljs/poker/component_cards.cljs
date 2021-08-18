(ns poker.component-cards
  (:require
   [nubank.workspaces.core  :as ws]
   [nubank.workspaces.card-types.react :as ct.react]
   [poker.components.panel  :refer [panel]]
   [poker.components.card   :refer [card card-back card-in-message]]
   [poker.components.table  :refer [table]]
   [poker.components.player-avatar :refer [player-avatar]]
   [poker.components.button :refer [raise-button check-button call-button bet-button fold-button]]
   [poker.components.stack  :refer [stack]]
   [reagent.core            :as reagent]))



(def avatar-url
  "https://avatars.githubusercontent.com/u/11796018?s=400&u=b0dcc2618d331a34a63417e1fef8a210649777a3&v=4")

(def example-players
  {1 {;; current player, so hole-cards are visible
      :name       "foo",
      :bets       [300],
      :avatar-url avatar-url,
      :hole-cards [[:s :a] [:h :t]],
      :stack      500,
      :seat       1,
      :position   "CO",
      :status     :player-status/wait-for-action},
   2 {:name       "bar",
      :bets       [300],
      :avatar-url avatar-url,
      :stack      500,
      :seat       2,
      :position   "BTN",
      :status     :player-status/wait-for-action},
   3 {:name       "baz",
      :bets       [500],
      :avatar-url avatar-url,
      :stack      500,
      :seat       5,
      :position   "SB",
      :status     :player-status/all-in},
   4 {:name       "baz",
      :bets       [500],
      :avatar-url avatar-url,
      :stack      500,
      :seat       3,
      :position   "BB",
      :status     :player-status/in-action},
   5 {:name       "dog",
      :bets       [nil],
      :avatar-url avatar-url,
      :stack      500,
      :seat       4,
      :position   "BB",
      :status     :player-status/fold}})

(def example-off-seat-players
  {6 {:name       "foo-off-seat",
      :bets       nil,
      :avatar-url avatar-url,
      :hole-cards nil,
      :stack      nil,
      :seat       nil,
      :position   nil,
      :status     :player-status/off-seat},
   7 {:name       "foo-off-seat-2",
      :bets       nil,
      :avatar-url avatar-url,
      :hole-cards nil,
      :stack      nil,
      :seat       nil,
      :position   nil,
      :status     :player-status/off-seat}})

(ws/defcard spade-ten (ct.react/react-card (reagent/as-element [card [:s :t]])))

(ws/defcard heart-jack (ct.react/react-card (reagent/as-element [card [:h :j]])))

(ws/defcard player-avatar-all-in
  (ct.react/react-card (reagent/as-element [player-avatar
                                            {:name     "dog",
                                             :avatar   avatar-url,
                                             :status   :player-status/all-in,
                                             :stack    500,
                                             :position "CO"}])))

(ws/defcard player-avatar-wait-for-action
  (ct.react/react-card (reagent/as-element [player-avatar
                                            {:name     "dog",
                                             :avatar   avatar-url,
                                             :status   :player-status/wait-for-action,
                                             :stack    500,
                                             :position "BTN"}])))

(ws/defcard player-avatar-in-action
  (ct.react/react-card (reagent/as-element [player-avatar
                                            {:name     "dog",
                                             :avatar   avatar-url,
                                             :status   :player-status/in-action,
                                             :stack    500,
                                             :position "BTN"}])))

(ws/defcard player-avatar-fold
  (ct.react/react-card (reagent/as-element [player-avatar
                                            {:name     "dog",
                                             :avatar   avatar-url,
                                             :status   :player-status/fold,
                                             :stack    500,
                                             :position "UTG"}])))

(ws/defcard player-wait-for-start
  (ct.react/react-card (reagent/as-element [player-avatar
                                            {:name     "dog",
                                             :avatar   avatar-url,
                                             :status   :player-status/wait-for-start,
                                             :stack    500,
                                             :position "UTG"}])))

;; (ws/defcard player-avatar-winner
;;   (ct.react/react-card (reagent/as-element [player-avatar
;;                                             {:name     "dog",
;;                                              :avatar   avatar-url,
;;                                              :status   :player-status-local/winner,
;;                                              :stack    500,
;;                                              :position "UTG"}])))


(ws/defcard player-avatar-winner
  (ct.react/react-card (reagent/as-element [player-avatar
                                            {:name           "dog",
                                             :props          {:player/avatar "🐡"},
                                             :status         :player-status-local/winner,
                                             :showdown-cards [[:h :a] [:h :k]],
                                             :stack          500,
                                             :position       "UTG"}])))
(ws/defcard button-raise
  (ct.react/react-card (reagent/as-element
                        [raise-button {:stack 1000, :street-bet 10, :pot 30, :on-raise js/alert}])))

(ws/defcard button-check
  (ct.react/react-card (reagent/as-element [check-button {:on-check #(js/alert "check!")}])))

(ws/defcard button-call
  (ct.react/react-card (reagent/as-element [call-button {:on-call #(js/alert "call!")}])))

(ws/defcard button-bet
  (ct.react/react-card (reagent/as-element [bet-button {:stack 1000, :pot 50, :on-bet js/alert}])))

(ws/defcard button-fold
  (ct.react/react-card (reagent/as-element [fold-button {:on-fold #(js/alert "fold!")}])))

(ws/defcard stack-206
  (ct.react/react-card
   (reagent/as-element
    [:div.scale-150 [stack {:value 206}]])))

(ws/defcard stack-33
  (ct.react/react-card (reagent/as-element [stack {:value 33}])))

(ws/defcard stack-300
  (ct.react/react-card (reagent/as-element [stack {:value 300}])))

(ws/defcard card-back-card (ct.react/react-card (reagent/as-element [card-back])))

(ws/defcard panel-no-btns
  (ct.react/react-card
   (reagent/as-element
    [:div.flex.flex-col.bg-gray-300.justify-end {:style {:width "50rem", :height "20rem"}}
     [panel
      {:current-player {:status     :player-status/wait-for-action,
                        :id         1,
                        :position   "CO",
                        :name       "hero",
                        :stack      10000,
                        :hole-cards [[:s :a] [:s :k]],
                        :bets       nil},
       :pots           [],
       :street-bet     30}]])))

(ws/defcard panel-can-fold
  (ct.react/react-card
   (reagent/as-element
    [:div.flex.flex-col.bg-gray-300.justify-end {:style {:width "50rem", :height "20rem"}}
     [panel
      {:current-player {:status     :player-status/in-action,
                        :id         1,
                        :name       "hero",
                        :stack      10000,
                        :position   "UTG",
                        :hole-cards [[:s :a] [:s :k]],
                        :bets       [30 10]},
       :pot            {:player-ids #{1}, :value 500},
       :street-bet     30}]])))

(ws/defcard message-with-cards
  (ct.react/react-card
   (reagent/as-element
    [:div
     [:span "cards"]
     [:span
      [card-in-message [:c :k]]
      [card-in-message [:c :t]]
      [card-in-message [:c :3]]]])))
