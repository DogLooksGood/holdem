(ns poker.components.table
  (:require
   [re-frame.core          :as re-frame]
   [poker.utils            :as u]
   [poker.components.card  :refer [card card-placeholder card-back]]
   [poker.components.stack :refer [stack]]
   [poker.components.player-avatar :refer [player-avatar]]
   [poker.events.game]
   [cljs.core.async        :as a]
   [reagent.core           :as r]
   [poker.logger           :as log]))


;; utilities

(def seat->class
  {1 {:root ["absolute" "left-1/2" "top-full" "md:left-1/5"],
      :bet  ["absolute" "right-1/2" "-top-12"]},

   2 {:root ["absolute" "left-0" "top-4/5" "md:top-1/3"],
      :bet  ["absolute" "bottom-0" "-right-6" "md:-right-12"]},

   3 {:root ["absolute" "top-1/6" "left-0" "md:top-0" "md:left-1/4"],
      :bet  ["absolute" "bottom-0" "-right-6" "md:-bottom-12" "md:right-1/4"]},

   4 {:root ["absolute" "left-1/2" "top-0" "md:left-1/2"],
      :bet  ["absolute" "left-1/2" "-bottom-12"]},

   5 {:root ["absolute" "left-full" "top-1/6" "md:top-0" "md:left-3/4"],
      :bet  ["absolute" "bottom-0" "-left-6" "md:-bottom-12" "md:left-1/2"]},

   6 {:root ["absolute" "left-full" "top-4/5" "md:top-1/3"],
      :bet  ["absolute" "bottom-0" "-left-6" "md:-left-12"]}})


(defn get-player-relative-position
  [players player-id]
  (->> players
       (keep-indexed (fn [idx p]
                       (when (= player-id (:id p))
                         (inc idx))))
       first))

;; event handler

(defn on-buyin
  [{:keys [seat]} _e]
  (re-frame/dispatch [:game/buyin {:seat seat}]))

;; renderer

(defn render-deal-card-animation
  [players]
  (let [deals (->> (concat players players)
                   (filter (comp #{:player-status/in-action :player-status/wait-for-action}
                                 :status))
                   (map (comp :root seat->class (partial get-player-relative-position players) :id))
                   (map-indexed (fn [idx cls]
                                  {:cls       cls,
                                   :idx       idx,
                                   :animated? false}))
                   (vec))
        card-deal-states* (r/atom deals)]
    (a/go-loop [[{:keys [idx], :as d} & ds] deals]
      (when d
        (swap! card-deal-states* assoc-in [idx :animated?] true)
        (a/<! (a/timeout 30))
        (re-frame/dispatch-sync [:sound/play :sound-event/deal-cards])
        (recur ds)))
    (fn []
      (let [card-deal-states @card-deal-states*]
        [:<>
         (for [{:keys [idx cls animated?]} card-deal-states]
           ^{:key (str idx)}
           [:div.absolute.deal-card
            {:class
             (if animated?
               cls
               ["top-1/2" "left-1/2"])}
            [:div.absolute.-top-12.-left-8.scale-75
             [card-back]]])]))))

(defn render-winner-award-1
  [delay-n value to-cls]
  (let [cls (r/atom nil)]
    (a/go (a/<! (a/timeout (+ 300 (* delay-n 500))))
     (reset! cls to-cls))
    ;; (log/info "render winner awards-1, " delay-n value to-cls)
    (fn []
      [:div.absolute.award
       {:class (if-let [c @cls]
                 (conj c "opacity-0")
                 ["top-1/2" "left-1/2" "md:top-1/2" "md:left-1/2"])}
       [:div.absolute.-top-12.w-24.h-24.flex.justify-center.items-center
        {:style {:left (str (- delay-n 3) "rem")}}
        [stack {:value value}]]])))

(defn render-winner-awards
  [{:keys [pots players awards]}]
  ;; (log/info "render winner awards, pots:" pots)
  ;; (log/info "render winner awards, awards:" awards)
  [:div
   (for [[idx {:keys [winner-ids player-ids value]}] (map-indexed vector pots)]
     ^{:key (str "pot-" idx)}
     [:div
      (for [id winner-ids]
        ^{:key (str "id-" id)}
        [render-winner-award-1
         idx
         value
         (-> (get-player-relative-position players id)
             seat->class
             :root)])])])

(defn render-return-bets
  [{:keys [return-bets players]}]
  [:div
   (for [[id value] return-bets]
     ^{:key (str id)}
     [render-winner-award-1
      0
      value
      (-> (get-player-relative-position players id)
          seat->class
          :root)])])

(defn render-static-cards
  [cards skip-n]
  [:div.flex.justify-start.items-end.w-80
   [:div.flex.justify-start
    (for [i (range skip-n)]
      ^{:key i}
      [:div.m-1 [card-placeholder]])]
   [:div.flex.justify-start
    (for [c (drop skip-n cards)]
      ^{:key c}
      [:div.m-1 [card c]])]])

(defn play-deal-card-sounds
  [delay-ms-list]
  (a/go-loop [[t & ts] delay-ms-list]
    (when t
      (a/<! (a/timeout t))
      (re-frame/dispatch-sync [:sound/play :sound-event/deal-cards])
      (recur ts))))

(defn render-community-cards-deal-all
  [_cards skip-n]
  (case skip-n
    0 (play-deal-card-sounds [0 2000 2000])
    3 (play-deal-card-sounds [0 2000])
    4 (play-deal-card-sounds [0])
    nil)
  (fn [cards skip-n]
    (case skip-n
      0 [:div.flex.justify-start.items-end.w-80.z-2
         [:div.new-card.flex
          (for [c (take 3 cards)]
            ^{:key c}
            [:div.m-1 [card c]])]
         [:div.new-card-2 [:div.m-1 [card (nth cards 3)]]]
         [:div.new-card-3 [:div.m-1 [card (last cards)]]]]
      3 [:div.flex.justify-start.items-end.w-80.z-2
         [:div.flex
          (for [c (range 3)]
            ^{:key c}
            [:div.m-1 [card-placeholder]])]
         [:div.new-card [:div.m-1 [card (nth cards 3)]]]
         [:div.new-card-2 [:div.m-1 [card (last cards)]]]]

      4 [:div.flex.justify-start.items-end.w-80.z-2
         [:div.flex
          (for [c (range 4)]
            ^{:key c}
            [:div.m-1 [card-placeholder]])]
         [:div.new-card [:div.m-1 [card (last cards)]]]])))

(defn render-community-cards-deal-continue
  [_cards prev-n]
  (case prev-n
    3 (play-deal-card-sounds [2000 2000])
    4 (play-deal-card-sounds [2000])
    nil)
  (fn [cards prev-n]
    (case prev-n
      0 (render-community-cards-deal-all cards 0)
      3 [:div.flex.justify-start.items-end.w-80
         [:div.flex
          (for [c (take 3 cards)]
            ^{:key c}
            [:div.m-1 [card c]])]
         [:div.new-card-2 [:div.m-1 [card (nth cards 3)]]]
         [:div.new-card-3 [:div.m-1 [card (last cards)]]]]
      4 [:div.flex.justify-start.items-end.w-80
         [:div.flex
          (for [c (take 4 cards)]
            ^{:key c}
            [:div.m-1 [card c]])]
         [:div.new-card-2 [:div.m-1 [card (last cards)]]]]
      5 (render-static-cards cards 0))))

(defn render-community-cards
  [cards]
  (case (count cards)
    3 [:div.flex.justify-start.items-end.w-80
       [:div.flex.new-card
        (for [c cards]
          ^{:key c}
          [:div.m-1 [card c]])]]

    4 [:div.flex.justify-start.items-end.w-80
       [:div.flex
        (for [c (take 3 cards)]
          ^{:key c}
          [:div.m-1 [card c]])]
       [:div.new-card [:div.m-1 [card (last cards)]]]]

    5 [:div.flex.justify-start.items-end.w-80
       [:div.flex.justify-start
        (for [c cards]
          ^{:key c}
          [:div.m-1 [card c]])]]))

(defn render-pots
  [pots]
  (let [value (transduce (map :value) + 0 pots)]
    [:div.text-2xl.font-bold.text-gray-200.w-full.text-center
     (when-not (zero? value)
       (str "POT: " value))]))

(defn render-empty-seat
  []
  [:div.w-24.h-24.border.border-dashed.border-gray-300.opacity-50.rounded-full])

(defn render-player-last-message
  [msg]
  ^{:key (:message/id msg)}
  [:div.flex.flex-no-wrap.break-all.text-gray-800.bg-gray-100.bg-opacity-70.border.border-gray-800.p-1.text-2xs.message-popup-fade
   (let [content (:message/content msg)]
     (if (< 30 (count content))
       (str content "...")
       content))])

(defn render-player-avatar
  [{:keys       [name props stack status position showdown-cards hole-cards],
    :local/keys [game-status award is-current-player],
    :as         _player-state}]
  [player-avatar
   {:name              name,
    :status            status,
    :stack             stack,
    :props             props,
    :position          position,
    :award             award,
    :showdown-cards    showdown-cards,
    :hole-cards        hole-cards,
    :is-current-player is-current-player,
    :local/game-status game-status}])

(defn render-player-seat
  [{:keys [status], :as player-state}]
  [:div
   (if (nil? status)
     [render-empty-seat]
     [render-player-avatar player-state])])

(defn table-seat-list
  [{:keys [players]}]
  [:div.bg-gray-800.flex-1.flex.flex-col.items-stretch.p-12.text-white
   (let [cnt (count (filter :name players))]
     [:div]
     [:div.ml-3 "Players: " cnt])
   (for [{:keys [name props seat stack position]} players]
     ^{:key (str seat)}
     [:div
      (if name
        ;; player on seat
        [:div.m-3.w-full.flex.justify-around.items-center.h-12.border
         {:class (if name
                   ["bg-blue-800" "border-blue-900"]
                   ["bg-green-700" "border-green-800"])}
         [:div.w-24.flex.items-center
          [:div.text-2xl (:player/avatar props)]
          [:div.ml-2 name]]
         [:div.w-16.text-center (or position "-")]
         [:div.w-24 "Stack:" stack]]
        ;; empty seat
        [:div.m-3.w-full.flex.justify-center.items-center.h-12.border.bg-green-700.border-green-800.hover:bg-green-600.animate-pulse
         {:class    (if name
                      ["bg-blue-800" "border-blue-900"]
                      ["bg-green-700" "border-green-800"]),
          :on-click (partial on-buyin {:seat seat})}
         "Join!"])])])

;; Render table with 6 seats

(defn render-players
  [{:keys [status current-player players]}]
  (let [street (u/game-status->street status)]
    [:<>
     (for [[idx {:keys [bets id position], :local/keys [last-message], :as p}] (map-indexed
                                                                                vector
                                                                                players)]
       (let [class-map (-> (+ 1 idx)
                           (seat->class))
             is-current-player? (= (:id current-player) id)]
         ^{:key (str idx)}
         [:div
          {:class (class-map :root)}
          [:div.w-24.h-24.absolute.-top-12.-left-12
           [render-player-seat
            (assoc p
                   :local/is-current-player is-current-player?
                   :local/game-status       status)]
           (when street
             (when-let [bet (last bets)]
               [:div
                {:class (class-map :bet)}
                [stack {:value bet}]]))
           (when last-message
             [:div.absolute.inset-x-0.top-12.bottom-0
              [render-player-last-message last-message]])]]))]))

(defn render-cards-and-pots
  [{:keys [runner-cards-deals community-cards awards pots]}]
  [:div.scale-75.absolute {:class ["top-1/2" "left-1/2"]}
   [:div.w-80.h-80.absolute.-left-40.-top-48.md:-top-36.flex.flex-col.justify-end.scale-75.md:scale-100
    (cond
      (= 2 (count runner-cards-deals))
      [:<>
       [render-static-cards (first runner-cards-deals) (count community-cards)]
       [render-community-cards-deal-continue (last runner-cards-deals)
        (count community-cards)]]

      (= 1 (count runner-cards-deals))
      [render-community-cards-deal-continue (first runner-cards-deals)
       (count community-cards)]

      (seq community-cards)
      [render-community-cards community-cards])
    ;; render pots
    [:div.self-stretch.flex.justify-center.items-center.h-20.w-full
     (when-not awards
       [render-pots pots])]]])

(defn table-6
  [{:keys [awards players runner-cards-deals return-bets
           community-cards pots current-player status]}]
  [:div.flex-1.flex.items-stretch.p-16.bg-transition
   [:div.flex-1.relative
    [render-players
     {:players        players,
      :current-player current-player,
      :status         status}]
    [render-cards-and-pots
     {:awards             awards,
      :pots               pots,
      :community-cards    community-cards,
      :runner-cards-deals runner-cards-deals}]
    (when (seq awards)
      [render-winner-awards
       {:pots    pots,
        :players players,
        :awards  awards}])
    (when (seq return-bets)
      [render-return-bets
       {:players     players,
        :return-bets return-bets}])
    (when (= status :game-status/preflop)
      [render-deal-card-animation players])]])

(defn table-loading
  []
  [:div.bg-gray-600.flex-1.flex.items-stretch.p-12
   [:div.bg-blue-600.border.border-4.border-blue-900.flex-1.rounded-full.flex.justify-center.items-center
    [:div.text-white.font-2xl "LOADING"]]])

(defn table
  [{:keys [size current-player], :or {size 6}, :as props}]
  (case size
    6
    (if (= :player-status/off-seat (:status current-player))
      [table-seat-list props]
      [table-6 props])
    [table-loading]))
