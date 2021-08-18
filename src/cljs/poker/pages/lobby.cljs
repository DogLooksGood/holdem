(ns poker.pages.lobby
  "Page for lobby.

  Create & join games."
  (:require
   [re-frame.core :as re-frame]
   [poker.events.lobby]
   [poker.subs.lobby]
   [poker.subs.game]
   [clojure.string :as str]))

(defn lobby-page-init
  "Query for all games."
  []
  (re-frame/dispatch [:lobby/list-games])
  (re-frame/dispatch [:lobby/list-players]))

(defn on-create-game
  [e]
  (let [es    (aget e "target" "elements")
        title (aget es "title" "value")]
    (when-not (str/blank? title)
      (re-frame/dispatch [:lobby/create-game {:game/title title}]))
    (.preventDefault e)))

(defn on-join-game
  [game-id e]
  (re-frame/dispatch [:lobby/join-game {:game/id game-id}]))

(defn lobby-page
  []
  (let [games*       (re-frame/subscribe [:lobby/games])
        player-ids*  (re-frame/subscribe [:lobby/online-player-ids])
        player-info* (re-frame/subscribe [:player/info])]
    (fn lobby-page-render []
      (let [games           @games*
            player-id       (-> @player-info*
                                :player/id)
            sorted-games    (sort-by
                             (juxt (comp - count :game/players)
                                   :game/title)
                             games)]
        [:div.h-screen.w-screen.flex.flex-col.justify-start.items-center.text-gray-900
         [:h1.text-lg.font-bold "Lobby"]
         [:div
          "Hello, "
          (:player/name player-id)]
         [:div.w-48.bg-gray-500.h-1.m-2]
         [:h3 "Available rooms:"]
         [:div.p-2.border.border-gray-700.w-52.flex.flex-col.items-stretch
          (when-not (seq sorted-games) "No games!")
          (for [{:game/keys [id players title]} sorted-games]
            ^{:key id}
            (let [inside (some #(= player-id %) players)]
              [:div.m-1.border.border-gray-700.flex.flex-col.justify-between.items-stretch.hover:border-blue-500
               {:on-click
                (partial on-join-game id)}
               [:div.flex.justify-between.items-center.p-1
                {:class (if inside ["bg-blue-200"] ["bg-gray-200"])}
                [:div title]
                [:div.w-6.h-6.rounded-full.text-white.flex.place-content-center
                 {:class (if inside ["bg-blue-500"] ["bg-gray-500"])}
                 (count players)]]
               [:div.flex.p-1
                (for [{:player/keys [name]} players]
                  ^{:key name}
                  [:div.border-sm.bg-gray-300.rounded-sm.px-2.m-1.text-2xs name])]]))]
         [:div.w-48.bg-gray-500.h-1.m-2]
         [:form.flex.flex-col {:on-submit on-create-game}
          [:input.border-gray-700.placeholder-gray-700
           {:auto-focus true, :type "text", :name "title", :placeholder "Title"}]
          [:button.mt-2.py-2.border.border-gray-700.hover:bg-gray-800.hover:text-white "New Room"]]
         [:div.w-48.bg-gray-500.h-1.m-2]
         [:button.mt-2.py-2.px-4.hover:bg-gray-800.text-gray-700.hover:text-gray-200.text-2xl
          {:on-click #(re-frame/dispatch [:router/push-state :ladder])}
          "Ladder 🪜"]
         [:div.w-48.bg-gray-500.h-1.m-2]
         [:h3 "Online players:"]
         [:p
          (for [id @player-ids*]
            ^{:key id}
            [:span.mx-1.bg-gray-200.rounded-md.p-1 (:player/name id)])]]))))
