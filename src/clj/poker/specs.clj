(ns poker.specs
  "Specs of application."
  (:require
   [clojure.spec.alpha :as s]))

(s/check-asserts true)

;; game specs

(def suits #{:d :s :h :c})

(def kinds #{:a :2 :3 :4 :5 :6 :7 :8 :9 :t :j :q :k})

(s/def ::game-type #{:long :short})
(s/def ::sb integer?)
(s/def ::bb integer?)
(s/def ::action-secs integer?)
(s/def ::extra-secs integer?)
(s/def ::seats-count integer?)
(s/def ::min-buyin integer?)
(s/def ::max-buyin integer?)

(s/def ::game-opts
  (s/keys :req-un [::sb ::bb ::action-secs ::extra-secs ::seats-count ::min-buyin ::max-buyin]))

(s/def ::player-status
  #{:player-status/wait-for-bb ; wait BB to join the game
    :player-status/wait-for-action ; wait for the first action
    :player-status/acted ; acted, but can have more action
    :player-status/in-action ; player in action, receiving player event
    :player-status/wait-for-start ; wait for game start
    :player-status/off-seat ; off seat, wait for buyin
    :player-status/all-in ; player all-in, wait for show
    :player-status/exhausted ; player with no stack, but still not off-seat
    :player-status/open      ; open cards before showdown
    :player-status/winner    ; player is the winner
    :player-status/leave     ; player leave game
    :player-status/join-bet  ; player will bet at next game
   })

;; account specs
(s/def :player/name string?)
(s/def :player/id (s/keys :req [:player/name]))
(s/def :player/balance integer?)
(s/def :player/avatar string?)
(s/def :player/token uuid?)
(s/def :game/id uuid?)
(s/def :game/title string?)
