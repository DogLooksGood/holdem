(ns poker.game.engine
  "Implementations of game engine."
  (:require
   [poker.game.event-handler :as eh]
   [poker.game.protocols     :as p]
   [poker.specs              :as specs]
   [clojure.spec.alpha       :as s]
   [clojure.tools.logging    :as log]))

(def default-game-opts
  {:sb          100,
   :bb          200,
   :action-secs 30,
   :extra-secs  60,
   :seats-count 6,
   :min-buyin   5000,
   :max-buyin   20000
   :max-stack   30000})

(def default-engine-attrs
  {:status             :game-status/idle,
   :btn-seat           nil,
   :in-game-player-ids [],
   :players            {},
   :cards              nil,
   :community-cards    [],
   :pots               nil,
   :street-bet         nil,
   :min-raise          nil,
   :scheduled-events   nil,
   :next-events        [],
   :showdown           nil,
   :awards             nil,
   :return-bets        nil,
   :runner             nil,
   :player-action      nil,
   :network            nil,
   :ladder-events      nil,
   :action-id          nil})

(defn apply-game-event*
  [this event]
  {:pre  [(map? this) (vector? event)],
   :post [(map? %)]}
  (eh/apply-game-event this event))

(defrecord GameEngine
  [;; game engine status
   status
   ;; current button seat: integer
   btn-seat
   ;; player-ids in this game: [player-id]
   in-game-player-ids
   ;; player data: player-id -> Player, check `poker.game.model/Player`
   players
   ;; game options, check `default-game-opts`
   opts
   ;; game properties, contains `game/id` `game/title`
   props
   ;; remain cards
   cards
   ;; bet for this street
   street-bet
   ;; next event dispatch to engine immediately or after a timeout
   next-events
   ;; list of events to execute after each timeout
   schedule-events
   ;; pots: [Pot], check `poker.game.model/Pot`
   pots
   ;; public cards for all player
   community-cards
   ;; minimal raise value
   min-raise
   ;; recent hand showdown: player-id -> {category, value, hole-cards, picks}
   showdown
   ;; recent hand awards: player-id -> stack
   awards
   ;; returned bets, the parts that no player have enough stack to call
   return-bets
   ;; runner states
   runner
   ;; recent player action
   player-action
   ;; uuid generated when ask player to act
   action-id
   ;; network error status, default to nil, can be `network-error/dropout`
   network-error
   ;; ladder events
   ladder-events]
  p/IEngine
  ;; FIXME, return boolean instead of throwing error in validate-event.
  (valid-event? [this event]
    (if (nil? event)
      false
      (try
        (eh/validate-game-event this event)
        true
        (catch clojure.lang.ExceptionInfo e
          (log/warnf (ex-message e)))
        (catch Exception e
          (log/errorf e "unexpected error")
          false))))
  (validate-event! [this event] (eh/validate-game-event this event))
  (apply-event [this event]
    ;; (log/debugf "apply game event: %s" (prn-str event))
    (try
      (apply-game-event* this event)
      (catch Exception ex
        (log/errorf ex "unexpected error")
        this)))
  (next-event [_this] (first next-events))
  (has-next-event? [_this] (boolean (seq next-events)))
  (pop-next-event [this] (update this :next-events subvec 1))

  (list-scheduled-events [_this] schedule-events)
  (flush-scheduled-events [this] (assoc this :schedule-events nil))

  (player-action [_this] player-action)
  (has-player-action? [_this] (some? player-action))
  (pop-player-action [this] (assoc this :player-action nil))

  (list-ladder-events [_this] ladder-events)
  (flush-ladder-events [this] (assoc this :ladder-events #{})))

(defn make-game-engine
  "Make a game engine with options."
  [props game-opts]
  {:pre [(s/assert ::specs/game-opts game-opts)]}
  (map->GameEngine (assoc default-engine-attrs
                          :props props
                          :opts  game-opts)))
