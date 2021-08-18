(ns poker.web.handler
  (:require
   [hiccup.core           :as html]
   [ring.util.response    :as resp]
   [hiccup.page           :as page]
   [hiccup.util           :as html-util]
   [ring.middleware.anti-forgery]
   [poker.account         :as account]
   [poker.lobby           :as lobby]
   [poker.game            :as game]
   [poker.chat            :as chat]
   [poker.ladder          :as ladder]
   [clojure.tools.logging :as log]
   [clojure.core.async    :as a]
   [poker.system.web-pub  :refer [pub-bus]]
   [poker.web.ws          :as ws]
   [clojure.java.io       :as io]
   [clojure.string        :as str]))

(defn format-ex [ex] {:error (ex-message ex), :data (ex-data ex)})

(def index-hydration-template (str/trim-newline (slurp (io/resource "index-ssr.html"))))

(defn game-index-page
  [_req]
  {:status 200,
   :body   (let [csrf-token
                 (force
                  ring.middleware.anti-forgery/*anti-forgery-token*)]
             (html/html
              [:html
               [:head
                [:title "Holdem"]
                [:meta
                 {:name    "viewport",
                  :content "width=device-width, initial-scale=1.0"}]
                [:meta {:charset "UTF-8"}]
                (page/include-css "css/app.css")]
               [:body
                [:div#sente-csrf-token {:data-csrf-token csrf-token}]
                [:div#ssr-enabled]
                [:div#app
                 (html-util/as-str index-hydration-template)]
                (page/include-js "js/app.js")]]))})

(defn signup
  [{:keys [params session]}]
  (try
    (let [{:player/keys [token id]} (account/signup! params)]
      (-> (resp/response {:player/token token, :player/id id})
          ;; Old session contains CSRF token
          (assoc :session (assoc session :uid id))))
    (catch Exception ex
      (resp/response (format-ex ex)))))

(defn auth
  [{:keys [params session]}]
  (try
    (let [{:player/keys [token id]} (account/auth-player-by-token! (:token params))]
      (-> (resp/response {:player/token token, :player/id id})
          (assoc :session (assoc session :uid id))))
    (catch Exception ex
      (resp/response (format-ex ex)))))

(defn alive
  [req]
  (log/debugf "alive, session: %s" (prn-str (:session req)))
  {:status 200, :body "ok"})

(defmethod ws/handle-event :test/test
  [_ctx event]
  (log/debugf "test event: %s" (prn-str event)))

(defmethod ws/handle-event :lobby/create-game
  [_ctx {:keys [?data ?reply-fn]}]
  (try
    (log/debugf "player create game, ?data: %s" ?data)
    (let [token (:player/token ?data)
          {:player/keys [id avatar]} (account/auth-player-by-token! token)
          ret   (lobby/create-game! (assoc ?data
                                           :player/id    id
                                           :player/props {:player/avatar avatar}))]
      (a/>!! pub-bus [:lobby-output/updated])
      (?reply-fn ret))
    (catch Exception ex
      (?reply-fn (format-ex ex)))))

(defmethod ws/handle-event :lobby/list-games
  [_ctx {:keys [?reply-fn]}]
  (let [ret (lobby/list-games {})]
    (?reply-fn ret)))

(defmethod ws/handle-event :lobby/list-players
  [_ctx {:keys [?reply-fn]}]
  (let [ret (ws/list-all-uids)]
    (?reply-fn ret)))

(defmethod ws/handle-event :ladder/list-leaderboard
  [_ctx {:keys [?reply-fn]}]
  (let [ret (ladder/list-leaderboard)]
    (?reply-fn ret)))

(defmethod ws/handle-event :lobby/list-joined-games
  [_ctx {:keys [?reply-fn ?data]}]
  (let [ret (lobby/list-joined-games ?data)]
    (?reply-fn ret)))

(defmethod ws/handle-event :lobby/join-game
  [_ctx {:keys [?data ?reply-fn]}]
  (try
    (log/debugf "player join game: %s" ?data)
    (let [token (:player/token ?data)
          {:player/keys [id avatar]} (account/auth-player-by-token! token)
          ret   (lobby/join-game!
                 (assoc ?data :player/id id :player/props {:player/avatar avatar}))]
      (a/>!! pub-bus [:lobby-output/updated])
      (?reply-fn ret))
    (catch Exception ex
      (?reply-fn (format-ex ex)))))

;; Leave game or websocket disconnect
;; FIXME return correct status code.

(defmethod ws/handle-event :lobby/leave-game
  [_ctx {:keys [?data ?reply-fn]}]
  (try
    (log/debugf "player leave game: %s" ?data)
    (let [token     (:player/token ?data)
          player-id (:player/id (account/auth-player-by-token! token))
          ret       (lobby/leave-game! (assoc ?data :player/id player-id))]
      (a/>!! pub-bus [:lobby-output/updated])
      (?reply-fn ret))
    (catch Exception ex
      (?reply-fn (format-ex ex)))))

(defmethod ws/handle-event :chsk/uidport-open
  [_ctx {:keys [uid]}]
  (log/debugf "player connected: %s" uid)
  (a/>!! pub-bus [:lobby-output/updated]))

(defmethod ws/handle-event :chsk/uidport-close
  [_ctx {:keys [?data uid]}]
  (try
    (log/debugf "player leave game due to websocket disconnect: %s" ?data)
    (lobby/leave-all-games! {:player/id uid})
    (a/>!! pub-bus [:lobby-output/updated])
    (catch Exception ex
      (log/error ex))))

(defmethod ws/handle-event :game/call
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "player %s call: %s" (:player/name uid) ?data)
    (let [{:keys [game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game [:game-event/player-call {:player-id uid}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/bet
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s bet: %s" (:player/name uid) ?data)
    (let [{:keys [bet game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game [:game-event/player-bet {:player-id uid, :bet bet}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/check
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s check: %s" (:player/name uid) ?data)
    (let [{:keys [game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game [:game-event/player-check {:player-id uid}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/fold
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s fold: %s" (:player/name uid) ?data)
    (let [{:keys [game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game [:game-event/player-fold {:player-id uid}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/join-bet
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s join-bet: %s" (:player/name uid) ?data)
    (let [{:keys [game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game [:game-event/player-join-bet {:player-id uid}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/musk
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s musk: %s" (:player/name uid) ?data)
    (let [{:keys [game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game [:game-event/player-musk {:player-id uid}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/raise
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s raise: %s" (:player/name uid) ?data)
    (let [{:keys [raise game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game
                                       [:game-event/player-raise {:player-id uid, :raise raise}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/request-deal-times
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s request deal times: %s" (:player/name uid) ?data)
    (let [{:keys [deal-times game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event game
                                       [:game-event/player-request-deal-times
                                        {:player-id  uid,
                                         :deal-times deal-times}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/buyin
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s buyin" (:player/name uid))
    (let [{:keys [seat game-id]} ?data
          game  (game/get-game game-id)
          stack 20000]
      (?reply-fn
       (game/send-game-event game
                             [:game-event/player-buyin
                              {:player-id uid,
                               :seat      seat,
                               :stack-add stack}])))
    (catch Exception ex
      (log/warnf ex "Handle event error"))))

(defmethod ws/handle-event :game/get-current-state
  [_cxt {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s get current state" (:player/name uid))
    (let [{:keys [game-id]} ?data
          game (game/get-game game-id)]
      (?reply-fn (game/send-game-event
                  game
                  [:game-event/player-get-current-state {:player-id uid}])))
    (catch Exception ex
      (log/errorf ex "Handle event error"))))

(defmethod ws/handle-event :message/new-message
  [_ctx {:keys [?data uid ?reply-fn]}]
  (try
    (log/debugf "Player %s send message" (:player/name uid))
    (let [{:keys [game-id content]} ?data
          params {:message/game-id game-id,
                  :message/content content,
                  :message/sender  uid}]
      (?reply-fn (chat/broadcast-new-message! params)))
    (catch Exception ex
      (log/errorf ex "Handle event error"))))
