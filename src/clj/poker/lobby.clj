(ns poker.lobby
  "Context module of lobby.

  Player create game & join game."
  (:require
   [clojure.spec.alpha :as s]
   [crux.api           :as crux]
   [poker.game         :as game]
   [poker.specs]
   [poker.system.db    :as db]
   [clojure.tools.logging :as log])
  (:import java.util.UUID))

(s/def ::create-game-params (s/keys :req [:player/id :game/title]))
(s/def ::join-game-params (s/keys :req [:player/id :game/id]))
(s/def ::leave-game-params (s/keys :req [:player/id :game/id]))
(s/def ::leave-all-games-params (s/keys :req [:player/id]))
(s/def ::list-joined-games-params (s/keys :req [:player/id]))

(defn throw-join-game-failed!
  []
  (throw (ex-info "Join game failed" {})))

(defn throw-leave-game-failed!
  []
  (throw (ex-info "Leave game failed" {})))

(defn assert-game-is-open!
  [game]
  (when (not= :game-status/open (:game/status game))
    (throw-join-game-failed!)))

(defn add-players-to-game
  [game-id players]
  (let [game (game/get-game game-id)]
    (doseq [{:player/keys [id props]} players]
      (game/send-game-event game
                            [:game-event/player-join
                             {:id    id,
                              :name  (:player/name id),
                              :props props}]))))

(defn remove-players-from-game
  [game-id ids]
  (log/infof "remove players %s from game %s" ids game-id)
  (let [game (game/get-game game-id)]
    (doseq [id ids]
      (game/send-game-event game
                            [:game-event/player-leave {:player-id id}]))))

(defn force-game-settlement
  [game-id]
  (let [game (game/get-game game-id)]
    (game/send-game-event game [:game-event/next-game {}])))

(defn create-game!
  "Create game will start a new game event loop in game registry
  and save the game data in database."
  [params]
  {:pre [(s/assert ::create-game-params params)]}
  (let [{:game/keys [title]} params
        id           (UUID/randomUUID)
        player-id    (:player/id params)
        player-props (:player/props params)
        item         {:game/title   title,
                      :game/players #{player-id},
                      :game/opts    {},
                      :game/creator player-id,
                      :game/status  :game-status/open,
                      :game/id      id,
                      :crux.db/id   id}]
    (log/infof "Player %s create game, game id: %s" player-id id)
    (crux/await-tx db/node (crux/submit-tx db/node [[:crux.tx/put item]]))
    (game/start-game {:game/id id, :game/title title} {})
    (add-players-to-game id [{:player/id player-id, :player/props player-props}])
    {:game/id id}))

(comment
  ;; usages: list-joined-games
  (list-joined-games {:player/id {:player/name "foo"}}))

(defn list-joined-games
  ([params] (list-joined-games '[*] params))
  ([query params]
   {:pre [(s/valid? ::list-joined-games-params params)]}
   (let [player-id (:player/id params)
         db        (crux/db db/node)
         eids      (->> (crux/q db
                                '{:find  [?g],
                                  :where [[?g :game/players ?p]],
                                  :in    [?p]}
                                player-id)
                        (mapv first))]
     (crux/pull-many db query eids))))

(comment
  ;; usages: list-games
  (list-games {})
  (list-games '[:game/creator] {}))

(defn list-games
  ([params] (list-games '[*] params))
  ([query _params]
   (let [db   (crux/db db/node)
         q    '{:find  [?g],
                :where [[?g :game/id _]]}
         eids (mapv first (crux/q db q))]
     (crux/pull-many db query eids))))

(comment
  ;; usages: get-game
  (get-game #uuid "f228369c-52d9-4683-b319-c9bcd8541fe6"))

(defn get-game
  ([id] (get-game '[*] id))
  ([query id]
   (crux/pull (crux/db db/node) query id)))

(comment
  ;; usages: join-game
  (join-game! {:game/id   #uuid "c0e5f840-4db7-4852-a0b4-81566b5c43c2",
               :player/id {:player/name "xxx"}}))

(defn join-game!
  [params]
  {:pre [(s/assert ::join-game-params params)]}
  (let [game-id      (:game/id params)
        player-id    (:player/id params)
        player-props (:player/props params)
        game         (crux/entity (crux/db db/node) game-id)
        new-game     (update game :game/players conj player-id)
        tx           (crux/submit-tx
                      db/node
                      [[:crux.tx/match game-id game]
                       [:crux.tx/put new-game]])]
    (if (->> tx
             (crux/await-tx db/node)
             (crux/tx-committed? db/node))
      (do (add-players-to-game game-id [{:player/id player-id, :player/props player-props}])
          {:game/id game-id})
      (throw-join-game-failed!))))

(defn leave-game!
  [params]
  {:pre [(s/assert ::leave-game-params params)]}
  (let [game-id        (:game/id params)
        player-id      (:player/id params)
        game           (doto (crux/entity (crux/db db/node) game-id)
                        (assert-game-is-open!))
        {:game/keys [players]} game
        rep-players    (disj players player-id)
        no-player-left (empty? rep-players)
        new-game       (cond-> (assoc game :game/players rep-players)
                         no-player-left
                         (assoc :game/status :game-status/closed))
        tx             (crux/submit-tx
                        db/node
                        [[:crux.tx/match game-id game]
                         (if no-player-left
                           [:crux.tx/delete game-id]
                           [:crux.tx/put new-game])])]
    (if (->> tx
             (crux/await-tx db/node)
             (crux/tx-committed? db/node))
      (do (remove-players-from-game game-id [player-id])
          (when no-player-left
            (game/stop-game game-id))
          :ok)
      (throw-leave-game-failed!))))

(defn leave-all-games!
  [params]
  {:pre [(s/assert ::leave-all-games-params params)]}
  (let [player-id (:player/id params)
        db        (crux/db db/node)
        game-ids  (->> (crux/q db
                               '{:find  [?g],
                                 :where [[?g :game/players ?p]],
                                 :in    [?p]}
                               player-id)
                       (map first))]
    (doseq [gid game-ids]
      (leave-game! {:game/id   gid,
                    :player/id player-id}))
    :ok))
