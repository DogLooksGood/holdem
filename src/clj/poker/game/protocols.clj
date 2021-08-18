(ns poker.game.protocols "Definitions of the game engine related protocols.")

(defprotocol IEngine
  "Game engine, process event for a table of game.
  Implemented as a state machine.

  Protocol functions:
  - process-event([this event])
  Process the event, return the new state.

  - get-next-event([this])
  Return the next event, the format is [event-type event-data & [timeout-secs]]"
  (validate-event! [engine event])
  (valid-event? [engine event])
  (apply-event [engine event])
  (next-event [engine])
  (has-next-event? [engine])
  (pop-next-event [engine])
  (list-scheduled-events [engine])
  (flush-scheduled-events [engine])
  (timeout-event [engine])
  (has-timeout-event? [engine])
  (pop-timeout-event [engine])
  (player-action [engine])
  (has-player-action? [engine])
  (pop-player-action [engine])
  (list-ladder-events [engine])
  (flush-ladder-events [engine]))

(defprotocol IEventLoop
  "Game event loop, maintain the input, output channels and game state(IEngine).

  - start([this])
  Start the event loop.

  - stop([this])
  Stop the event loop, return the final state.

  - get-input-ch([this])
  Get the channel for input, send event into this channel will trigger event processing.

  - get-output-ch([this])
  Get the channel for output, consuming this channel to get updated states."
  (start [event-loop engine])
  (stop [event-loop])
  (get-state [event-loop])
  (get-input-ch [event-loop])
  (get-output-ch [event-loop])
  (send-event [event-loop event]))

(defprotocol IGameRegistry
  "Game Registry, maintain game instances.

  - add-game([this id game])
  Add a game to this registry.

  - remove-game([this id])
  Remove game by ID.

  - get-game([this id])
  Get game by ID."
  (add-game [game-registry id game])
  (remove-game [game-registry id])
  (get-game [game-registry id]))
