(ns poker.account
  "Context module of account.

  Create player, player charge, login and logout.

  Currently, we use some predefined avatar."
  (:require
   [clojure.spec.alpha :as s]
   [poker.specs]
   [poker.system.db    :as db]
   [crux.api           :as crux]))

(s/def ::signup-params (s/keys :req [:player/name :player/avatar]))
(s/def ::auth-params (s/keys :req [:player/token]))

(defn throw-player-name-not-available!
  [name]
  (throw (ex-info "Player name not available" {:name name})))

(defn throw-player-token-invalid!
  [token]
  (throw (ex-info "Player token invalid" {:token token})))

(defn ->player-id [m] (select-keys m [:player/name]))

(comment
  ;; usages: signup! & get-player
  (signup! {:player/name "bar", :player/avatar "AVATAR"})
  (get-player [:player/name :player/avatar] {:player/name "foofoo"}))

(defn get-player [query id] (crux/pull (crux/db db/node) query id))

(defn signup!
  [params]
  {:pre [(s/assert ::signup-params params)]}
  (let [{:player/keys [name avatar]} params
        id        (->player-id params)
        token     (java.util.UUID/randomUUID)
        item      {:crux.db/id id,
                   :player/id id
                   :player/name name,
                   :player/balance 10000,
                   :player/avatar avatar,
                   :player/token token}
        tx        (crux/submit-tx db/node [[:crux.tx/match id nil] [:crux.tx/put item]])
        committed (->> tx
                       (crux/await-tx db/node)
                       (crux/tx-committed? db/node))]
    (if-not committed
      (throw-player-name-not-available! name)
      {:player/token token :player/id id})))

(comment
  ;; usages: auth-player-by-token!
  (auth-player-by-token! '[*] #uuid "e252a68b-3695-4317-a3c0-8675d2a1c4ee"))

(defn auth-player-by-token!
  ([token] (auth-player-by-token! '[*] token))
  ([query token]
   {:pre [(s/assert :player/token token)]}
   (let [db (crux/db db/node)
         q '{:find [?p]
             :where [[?p :player/token token]]
             :in [token]}
         ret (crux/q db q token)]
     (if (seq ret)
       (crux/pull db query (ffirst ret))
       (throw-player-token-invalid! token)))))

(comment
  ;; usages: list-players
  (list-players '[*] {}))

(defn list-players
  ([params] (list-players '[*] params))
  ([query _params]
   (let [db (crux/db db/node)
         q '{:find [?p]
             :where [[?p :player/id _]]}
         eids (map first (crux/q db q))]
     (crux/pull-many db query eids))))
