(ns poker.components.player-avatar-cards
  (:require
   [nubank.workspaces.core :as ws]
   [nubank.workspaces.card-types.react :as ct.react]
   [poker.components.player-avatar :refer [player-avatar]]
   [reagent.core :as reagent]))


(ws/defcard player-at-showdown
  (ct.react/react-card
   (reagent/as-element [player-avatar
                        {:name              "dog",
                         :avatar            "🐂",
                         :status            :player-status/acted,
                         :stack             500,
                         :showdown-cards    [[:c :a] [:c :k]],
                         :position          "CO",
                         :local/game-status :game-status/showdown}])))

(ws/defcard player-at-runner-prepare
  (ct.react/react-card
   (reagent/as-element [player-avatar
                        {:name              "dog",
                         :avatar            "🐂",
                         :status            :player-status/acted,
                         :stack             500,
                         :showdown-cards    [[:c :a] [:c :k]],
                         :position          "CO",
                         :local/game-status :game-status/runner-prepare}])))
