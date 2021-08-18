(ns poker.game.registry
  (:require
   [poker.game.protocols  :as p]
   [clojure.tools.logging :as log]
   [clojure.core.async    :as a]))

(defrecord MemGameRegistry [games event-ch]
  p/IGameRegistry
  (add-game [_this id game]
    (log/infof "Registry add game, id: %s" id)
    (when event-ch
      (a/pipe (-> game
                  :event-loop
                  :output)
              event-ch
              false))
    (swap! games assoc id game))
  (remove-game [_this id]
    (log/infof "Registry remove game, id: %s" id)
    (swap! games dissoc id))
  (get-game [_this id]
    (get @games id)))

(defn make-mem-game-registry
  [event-ch]
  (->MemGameRegistry (atom {}) event-ch))

(defn close-mem-game-registry
  [registry]
  (let [games @(:games registry)]
    (doseq [g games]
      (some->
       (:event-loop g)
       (p/stop)))))
