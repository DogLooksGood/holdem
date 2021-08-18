(ns poker.game
  "Context module for game engine & event loop."
  (:require
   [poker.game.event-loop      :as event-loop]
   [poker.game.protocols       :as p]
   [poker.game.engine          :as engine]
   [poker.system.game-registry :as game-registry]
   [clojure.tools.logging      :as log]))

(defrecord Game [id game-opts event-loop])

;; errors

(defn throw-game-not-found!
  [id]
  (throw (ex-info "Game not found"
                  {:id id, :available-ids (keys @(:games game-registry/registry))})))

(defn throw-invalid-game-event!
  [event]
  (throw (ex-info "Game event invalid" {:event event})))

;; assertions

(def player-event-type-set
  #{:game-event/player-bet
    :game-event/player-buyin
    :game-event/player-call
    :game-event/player-check
    :game-event/player-fold
    :game-event/player-raise
    :game-event/player-join
    :game-event/player-join-bet
    :game-event/player-leave
    :game-event/player-off-seat
    :game-event/player-musk
    :game-event/player-request-deal-times
    :game-event/player-get-current-state})

(defn assert-game-event!
  [game-event]
  (let [type (first game-event)]
    (when-not (player-event-type-set type)
      (throw-invalid-game-event! game-event))))

;; publics

(def default-game-opts engine/default-game-opts)

(defn start-game
  "Start a game with title and game-opts.

  Return the Game, which contains information and event-loop."
  [props opts]
  (let [{:game/keys [id]} props
        game-opts (merge default-game-opts opts)
        state     (engine/make-game-engine props game-opts)
        el        (event-loop/make-event-loop)
        game      (map->Game {:id id, :game-opts game-opts, :event-loop el})]
    (log/infof "start game with id: %s, game-opts: %s" id game-opts)
    (p/add-game game-registry/registry id game)
    (p/start el state)
    game))

(defn get-game
  [id]
  (or
   (p/get-game game-registry/registry id)
   (throw-game-not-found! id)))

(defn stop-game
  [id]
  (let [game (get-game id)]
    (log/infof "stop game with id: %s" id)
    (p/remove-game game-registry/registry id)
    (p/stop (:event-loop game))))

(defn get-game-input-ch [game] (p/get-input-ch (:event-loop game)))

(defn get-game-output-ch [game] (p/get-output-ch (:event-loop game)))

(defn send-game-event
  [game event]
  (assert-game-event! event)
  (p/send-event (:event-loop game) event)
  :ok)
