(ns poker.pages.game
  "Page for game scene."
  (:require
   [reagent.core          :as r]
   [poker.utils           :refer [rotate-by]]
   [re-frame.core         :as re-frame]
   [poker.components.table :refer [table]]
   [poker.components.panel :refer [panel]]
   [poker.components.help :refer [help]]
   [poker.components.history-popup :refer [history-popup]]
   [poker.logger          :as log]
   [poker.mobile          :refer [setup-for-mobile]]
   [poker.subs.game]))

;; utilities

(defn sort-players-for-display
  "Return sorted player list in tables' order."
  [players current-player-id table-size]
  (let [current-seat    (get-in players [current-player-id :seat])
        seats           (cond->> (range 1 (inc table-size))
                                 current-seat
                                 (rotate-by #(= current-seat %)))
        seat->player    (->> players
                             (vals)
                             (map (juxt :seat identity))
                             (into {}))
        players-on-seat (->> seats
                             (map #(get seat->player % {:seat %})))]
    players-on-seat))

(defn player-with-last-message
  "Add last-message to each player."
  [messages {:keys [id], :as player}]
  (if-let [msg (some #(when (= id (:message/sender %)) %) messages)]
    (if (< (inst-ms (js/Date.)) (+ (:message/timestamp msg) 5000))
      (assoc player :local/last-message msg)
      player)
    player))

(defn player-with-awards
  "Add award value to each player."
  [awards {:keys [id], :as player}]
  (if-let [award (get awards id)]
    (assoc player :local/award award)
    player))

;; event handlers

(defn on-leave
  []
  (re-frame/dispatch [:lobby/leave-game]))

(defn on-get-current-state
  [{:keys [game-id], :as props}]
  (log/info "on get current state" props)
  (re-frame/dispatch [:game/get-current-state {:game-id (uuid game-id)}]))

;; renderers
(defn game-page
  [{:keys [game-id]}]
  ;; game-id comes from path params, convert it to uuid format.
  (let [game-id      (uuid game-id)
        state*       (re-frame/subscribe [:game/state game-id])
        local-state* (re-frame/subscribe [:game/local-state game-id])
        messages*    (re-frame/subscribe [:chat/messages])
        {player-id :player/id} @(re-frame/subscribe [:player/info])
        popup*       (r/atom nil)
        close-popup  #(reset! popup* nil)
        toggle-popup (fn []
                       (swap! popup*
                         #(case %
                            :help    :history
                            :history :help
                            :history)))]
    (setup-for-mobile)
    (fn []
      (let [state (merge @state* @local-state*)
            {:keys [players
                    opts
                    street-bet
                    pots
                    community-cards
                    status
                    showdown
                    winner-awards
                    runner-cards-deals
                    has-all-in?
                    min-raise
                    return-bets]}
            state
            {:keys [seats-count]} opts
            table-players (->> (sort-players-for-display players
                                                         player-id
                                                         seats-count)
                               (mapv (comp (partial player-with-last-message @messages*)
                                           (partial player-with-awards winner-awards))))]
        (.log js/console "render game page:" state)

        [:div.h-full.w-full.flex.flex-col.select-none.overflow-hidden
         {:class (if has-all-in? ["bg-red-900"] ["bg-blue-900"])}
         [:div.absolute.top-0.left-0.p-4.text-gray-300.hover:text-red-400.cursor-pointer.z-40
          {:on-click
           on-leave}
          "Exit"]
         [:div.absolute.top-0.right-0.p-4.text-gray-300.hover:text-green-400.cursor-pointer.z-40
          {:on-click toggle-popup}
          "History/Help"]
         (when @popup*
           [:div.absolute.bottom-0.left-0.right-0.top-0.bg-blue-900.flex.flex-col.justify-start.items-stretch.text-gray-300.z-50.p-4.overflow-scroll
            (case @popup*
              :history [history-popup {:on-close close-popup, :on-toggle toggle-popup}]
              :help    [help {:on-close close-popup, :on-toggle toggle-popup}])])
         (when opts
           (let [current-player (get players player-id)]
             [:<>
              [table
               {:size               seats-count,
                :players            table-players,
                :pots               pots,
                :runner-cards-deals runner-cards-deals,
                :community-cards    community-cards,
                :status             status,
                :awards             winner-awards,
                :return-bets        return-bets,
                :current-player     (get players player-id),
                :has-all-in?        has-all-in?,
                :showdown           showdown}]
              [panel
               {:status         status,
                :messages       @messages*,
                :current-player current-player,
                :players        (vals players),
                :opts           opts,
                :pots           pots,
                :min-raise      min-raise,
                :street-bet     street-bet}]]))]))))
