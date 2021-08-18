(ns poker.components.player-avatar
  (:require
   [poker.components.card :refer [card-back card sort-cards]]
   [goog.string :as gstr]
   [poker.utils :as u]))

(defn format-name
  [name]
  (let [cnt (count name)]
    (cond
      (< cnt 8)  [:span.text-md.font-bold name]
      (< cnt 12) [:span.text-sm.font-bold name]
      :else      [:span.text-sm.font-bold (subs name 0 9) "..."])))

(defn render-position
  [position]
  [:div.absolute.bottom-0.left-0.scale-75
   (when (#{"BTN" "BTN SB" "BB" "SB"} position)
     (case position
       ("BTN" "BTN SB")
       [:div.w-6.h-6.flex.justify-center.items-center.text-sm.font-bold.bg-yellow-300.text-black.rounded-full.border.border-black
        "D"]
       "BB"
       [:div.w-6.h-6.flex.justify-center.items-center.text-xs.font-bold.bg-gray-300.text-black.rounded-full.border.border-black
        "BB"]
       "SB"
       [:div.w-6.h-6.flex.justify-center.items-center.text-xs.font-bold.bg-gray-300.text-black.rounded-full.border.border-black
        "SB"]))])

(defn render-avatar
  [{:player/keys [avatar]}]
  [:div.w-full.h-full.flex.justify-center.items-center {:style {:font-size "3em"}}
   avatar])

(defn render-showdown-cards
  [cards use-flip-animate?]
  (let [[c1 c2] (sort-cards cards)]
    [:<>
     [:div.absolute.bottom-0.left-0
      {:class (when use-flip-animate? ["showdown-card"])}
      [:div.scale-75.-rotate-6
       [card c1]]]
     [:div.absolute.bottom-0.right-0
      {:class (when use-flip-animate? ["showdown-card"])}
      [:div.scale-75.rotate-6
       [card c2]]]]))

(defn render-showdown-fold-cards
  [cards]
  (let [[c1 c2] (sort-cards cards)]
    [:div.absolute.top-0.left-0.right-0.bottom-0.overflow-hidden
     [:div.absolute.inset-x-1.showdown-fold-card
      [:div.scale-75.-rotate-6
       [card c1]]]
     [:div.absolute.inset-x-9.showdown-fold-card
      [:div.scale-75.rotate-6
       [card c2]]]]))

(defn render-hole-cards
  []
  [:<>
   [:div.absolute.bottom-0.left-0
    [:div.scale-75.-rotate-6
     [card-back]]]
   [:div.absolute.bottom-0.right-0
    [:div.scale-75.rotate-6
     [card-back]]]])

(defn render-fold-cards
  []
  [:div.absolute.top-0.left-0.right-0.bottom-0.overflow-hidden
   [:div.absolute.inset-y-8.inset-x-1
    [:div.scale-75.-rotate-6
     [card-back]]]
   [:div.absolute.inset-y-8.inset-x-9
    [:div.scale-75.rotate-6
     [card-back]]]])

(defn render-cards
  [{:keys [showdown-cards hole-cards game-status is-current-player]}]
  (cond
    is-current-player
    [render-showdown-cards hole-cards true]

    showdown-cards
    [render-showdown-cards showdown-cards
     (#{:game-status/runner-prepare :game-status/showdown-settlement} game-status)]

    :else
    [render-hole-cards]))

(defn render-wait-for-bb
  [{:keys [props name]}]
  [:div.w-24.h-24.relative
   [:div.rounded-full.absolute.top-0.left-0.bottom-0.right-0.opacity-50.bg-gray-500.overflow-hidden]
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-gray-500.bg-gray-500
    [render-avatar props]]
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-gray-600.border.border-gray-500.text-center.text-white.leading-tight
    [:div.text-sm.font-bold (format-name name)]
    [:div.text-2xs "Wait for BB"]]])

(defn render-off-seat
  [{:keys [stack props name is-current-player]}]
  [:div.w-24.h-24.relative
   [:div.rounded-full.absolute.top-0.left-0.bottom-0.right-0.opacity-50.bg-gray-500.overflow-hidden.text-5xl.text-red-500.flex.justify-center.items-center
    (gstr/unescapeEntities "&#10060;")]
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-gray-500.bg-gray-500
    [render-avatar props]]
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-gray-600.border.border-gray-500.text-center.text-white.leading-tight
    [:div.text-sm.font-bold (format-name name)]
    [:div.text-2xs (cond-> stack (not is-current-player) u/format-stack-value)]]])

(defn render-wait-for-start
  [{:keys [stack props name is-current-player]}]
  [:div.w-24.h-24.relative
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-gray-500.bg-gray-500.overflow-hidden
    [render-avatar props]]
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-gray-600.border.border-gray-500.text-center.text-white.leading-tight
    [:div.text-sm.font-bold (format-name name)]
    [:div.text-2xs (cond-> stack (not is-current-player) u/format-stack-value)]]])

(defn render-in-action
  [{:keys       [name props stack showdown-cards is-current-player position hole-cards],
    :local/keys [game-status]}]
  [:div.w-24.h-24.relative
   ;; Avatar
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-green-700.bg-green-500.overflow-hidden
    [render-avatar props]]
   ;; Hole Cards
   [render-cards
    {:showdown-cards    showdown-cards,
     :hole-cards        hole-cards,
     :is-current-player is-current-player,
     :game-status       game-status}]
   ;; Name & Stack
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-green-600.border.border-green-700.text-center.text-white.leading-tight
    [render-position position]
    [:div.text-sm.font-bold (format-name name)]
    [:div.text-2xs (cond-> stack (not is-current-player) u/format-stack-value)]]
   [:div.absolute.-bottom-2.left-0.right-0.h-1.bg-gray-700.rounded-full.flex.justify-start
    ^{:key (str game-status)}
    [:div.countdown-bar]]])

(defn render-default
  [{:keys       [name showdown-cards position props stack is-current-player hole-cards],
    :local/keys [game-status]}]
  (.log js/console "render avatar default" game-status)
  [:div.w-24.h-24.relative
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-gray-700.bg-gray-700.overflow-hidden
    [render-avatar props]]
   [render-cards
    {:showdown-cards    showdown-cards,
     :hole-cards        hole-cards,
     :is-current-player is-current-player,
     :game-status       game-status}]
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-gray-700.border.border-gray-800.text-center.text-white.leading-tight
    [render-position position]
    [:div.text-sm.font-bold (format-name name)]
    [:div.text-2xs (cond-> stack (not is-current-player) u/format-stack-value)]]])

(defn render-all-in
  [{:keys       [name stack showdown-cards props is-current-player position hole-cards],
    :local/keys [game-status]}]
  [:div.w-24.h-24.relative
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-red-700.bg-red-600.overflow-hidden
    [render-avatar props]]
   [render-cards
    {:showdown-cards    showdown-cards,
     :hole-cards        hole-cards,
     :is-current-player is-current-player,
     :game-status       game-status}]
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-red-600.border.border-red-800.text-center.text-white.leading-tight
    [render-position position]
    [:div.text-sm.font-bold (format-name name)]
    [:div.font-bold.text-2xs
     (if (zero? stack)
       "ALL-IN!"
       (cond-> stack (not is-current-player) u/format-stack-value))]]])

(defn render-fold
  [{:keys [name props stack is-current-player position hole-cards]}]
  [:div.w-24.h-24.relative
   [:div.rounded-full.absolute.top-0.left-0.bottom-0.right-0.opacity-50.bg-gray-500]
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-gray-500.bg-gray-500.overflow-hidden
    [render-avatar props]]
   (if is-current-player
     [render-showdown-fold-cards hole-cards]
     [render-fold-cards])
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-gray-600.border.border-gray-500.text-white.text-center.leading-tight
    [render-position position]
    [:div.text-sm.font-bold (format-name name)]
    [:div.text-2xs (cond-> stack (not is-current-player) u/format-stack-value)]]])

(defn render-winner
  [{:keys [props showdown-cards award is-current-player]}]
  [:div.w-24.h-24.relative
   (when-not is-current-player
     (when showdown-cards
       [render-showdown-cards showdown-cards]))
   [:div.border-4.rounded-full.w-full.h-full.shadow-xl.border-yellow-500.bg-black.overflow-hidden
    [render-avatar props]]
   [:div.rounded-md.absolute.bottom-0.left-0.w-full.h-7.bg-black.border.border-2.border-yellow-400.text-center.text-white.leading-tight
    [:div.text-yellow-200.text-lg.font-bold.animate-pulse
     {:style {:text-shadow "#000 0px 0px 4px"}}
     (str "+" award)]]])

(defn player-avatar
  [player-state]
  (let [{:keys [status]} player-state]
    (case status
      ;; client specific
      :player-status-local/winner   (render-winner player-state)
      ;; global status
      :player-status/in-action      (render-in-action player-state)
      (:player-status/wait-for-action :player-status/acted) (render-default player-state)
      :player-status/all-in         (render-all-in player-state)
      :player-status/fold           (render-fold player-state)
      :player-status/leave          (render-off-seat player-state)
      (:player-status/wait-for-bb :player-status/join-bet) (render-wait-for-bb player-state)
      :player-status/wait-for-start (render-wait-for-start player-state)
      (.error js/console "Invalid player status:" status))))
