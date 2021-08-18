(ns poker.chat
  (:require
   [clojure.spec.alpha   :as s]
   [poker.lobby          :as lobby]
   [poker.system.web-pub :as web-pub]
   [clojure.core.async   :as a])
  (:import java.util.UUID
           java.time.Instant))

(s/def :message/content string?)
(s/def :message/game-id uuid?)
(s/def :message/sender any?)
(s/def :message/to-players coll?)

(s/def ::broadcast-new-message-params
  (s/keys :req [:message/game-id :message/content :message/sender]))

(defn broadcast-new-message!
  [params]
  {:pre [(s/valid? ::broadcast-new-message-params params)]}
  (let [{:message/keys [content game-id sender]} params
        {:game/keys [players]} (lobby/get-game [:game/players] game-id)
        message {:message/content    content,
                 :message/game-id    game-id,
                 :message/sender     sender,
                 :message/to-players players
                 :message/timestamp  (inst-ms (Instant/now))
                 :message/id         (UUID/randomUUID)}]
    (a/>!! web-pub/pub-bus [:chat-output/new-message message])
    :ok))
