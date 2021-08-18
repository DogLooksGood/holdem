(ns poker.events.sound
  (:require
   [re-frame.core :as re-frame]))

(def enable-sound true)

(defmulti play-sound
  (fn [_state [event-type _event-data]]
    event-type))

(def chip-audio (js/Audio. "audio/chip.mp3"))
(def call-audio (js/Audio. "audio/call.mp3"))
(def bet-audio (js/Audio. "audio/bet.mp3"))
(def raise-audio (js/Audio. "audio/raise.mp3"))
(def fold-audio (js/Audio. "audio/fold.mp3"))
(def check-audio (js/Audio. "audio/check.mp3"))
(def card-audio (js/Audio. "audio/card.mp3"))
(def win-audio (js/Audio. "audio/win.mp3"))
(def allin-audio (js/Audio. "audio/allin.mp3"))
(def message-audio (js/Audio. "audio/message.mp3"))

(defmethod play-sound :game-event/player-bet
  [state [_ {:keys [player-id]}]]
  (if (some-> (get-in state [:players player-id :stack])
              zero?)
    (do (.play allin-audio)
        (.play chip-audio))
    (do (.play bet-audio)
        (.play chip-audio))))

(defmethod play-sound :game-event/player-raise
  [state [_ {:keys [player-id]}]]
  (if (some-> (get-in state [:players player-id :stack])
              zero?)
    (do (.play allin-audio)
        (.play chip-audio))
    (do (.play raise-audio)
        (.play chip-audio))))

(defmethod play-sound :game-event/player-check
  [_ _]
  (.play check-audio))

(defmethod play-sound :game-event/player-fold
  [_ _]
  (.play fold-audio))

(defmethod play-sound :game-event/player-musk
  [_ _]
  (.play fold-audio))

(defmethod play-sound :game-event/player-call
  [_ _]
  (.play call-audio)
  (.play chip-audio))

(defmethod play-sound :game-event/flop-street
  [_ _]
  (.play card-audio))

(defmethod play-sound :game-event/turn-street
  [_ _]
  (.play card-audio))

(defmethod play-sound :game-event/river-street
  [_ _]
  (.play card-audio))

(defmethod play-sound :local/winner
  [_ _]
  (.play win-audio))

(defmethod play-sound :game-event/showdown
  [_ _]
  (.play card-audio))

(defmethod play-sound :game-event/runner-prepare
  [_ _]
  (.play card-audio))

(defmethod play-sound :chat-event/new-message
  [_ _]
  (.play message-audio))

(defmethod play-sound :sound-event/deal-cards
  [_ _]
  (.play card-audio))

(defmethod play-sound :default
  [_ _])

(re-frame/reg-fx :sound/play-event-sound
  (fn [{:keys [state event]}]
    (when enable-sound
      (play-sound state event))))

(re-frame/reg-event-fx :sound/play
  (fn [{:keys [db]} [_ k]]
    {:db db,
     :sound/play-event-sound {:event [k]}}))
