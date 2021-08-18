(ns poker.game.model
  "Definitions of data model.")

(defrecord Player [id name stack hole-cards bets seat status position props prev-stack])

(defrecord Pot [player-ids value winner-ids street])

(defrecord Runner [results player-deal-times deal-times])

(defrecord ScheduleEvent [timeout event])

(defn make-player-state
  "Initialize a player state.

  This state should be safe to read by client."
  [attrs]
  {:pre [(contains? attrs :id)]}
  (map->Player (merge {:stack 0, :status :player-status/off-seat} attrs)))

(defn make-pot
  [attrs]
  {:pre [(contains? attrs :player-ids) (contains? attrs :value) (contains? attrs :street)]}
  (map->Pot attrs))

(defn make-runner [attrs] (map->Runner (merge {:deal-times 1, :player-deal-times {}} attrs)))

(defn make-schedule-event
  [attrs]
  {:pre [(contains? attrs :timeout) (contains? attrs :event)]}
  (map->ScheduleEvent attrs))
