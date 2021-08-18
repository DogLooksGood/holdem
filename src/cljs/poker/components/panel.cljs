(ns poker.components.panel
  "Player's status is displayed in panel.

  Buttons are displayed depending on current state. "
  (:require
   [reagent.core          :as reagent]
   [re-frame.core         :as re-frame]
   [poker.components.message-box :refer [message-box]]
   [poker.components.button :refer [raise-button
                                    check-button
                                    call-button
                                    bet-button
                                    fold-button
                                    reveal-button
                                    musk-button
                                    join-bet-button
                                    runner-times-button]]))

(defn get-buttons-set
  [players current-player street-bet]
  (when (= :player-status/in-action (:status current-player))
    (let [{:keys [bets]} current-player
          this-bet       (last bets)]
      (cond-> #{}
        (and (nil? street-bet) (nil? this-bet))
        (conj :check :bet)

        (and street-bet (= this-bet street-bet))
        (conj :check :raise)

        (< this-bet street-bet)
        (conj :fold :call)

        (and (< this-bet street-bet)
             (some (comp #{:player-status/acted :player-status/wait-for-action} :status) players))
        (conj :raise)))))

(defn on-fold
  []
  (re-frame/dispatch [:game/fold]))

(defn on-check
  []
  (re-frame/dispatch [:game/check]))

(defn on-call
  []
  (re-frame/dispatch [:game/call]))

(defn on-bet
  [bet]
  (re-frame/dispatch [:game/bet {:bet bet}]))

(defn on-raise
  [raise]
  (re-frame/dispatch [:game/raise {:raise raise}]))

(defn on-musk
  []
  (re-frame/dispatch [:game/musk]))

(defn on-join-bet
  []
  (re-frame/dispatch [:game/join-bet]))

(defn on-request-deal-times
  [deal-times]
  (re-frame/dispatch [:game/request-deal-times {:deal-times deal-times}]))

(defn bet-button-group
  [{:keys [buttons-set bet pot-val stack street-bet min-raise opts]}]
  (when buttons-set
    [:div.w-full.h-full.flex.flex-col.justify-end.items-stretch
     (when (:raise buttons-set)
       [raise-button
        {:min-raise  min-raise,
         :pot        pot-val,
         :sb         (:sb opts),
         :bb         (:bb opts),
         :stack      stack,
         :street-bet street-bet,
         :bet        bet,
         :on-raise   on-raise}])
     (when (:bet buttons-set)
       [bet-button
        {:pot    pot-val,
         :stack  stack,
         :on-bet on-bet,
         :bb     (:bb opts)}])
     [:div.flex.w-full
      (when (:call buttons-set)
        [call-button {:on-call on-call, :street-bet street-bet, :bet bet, :stack stack}])
      (when (:fold buttons-set)
        [fold-button {:on-fold on-fold}])
      (when (:check buttons-set)
        [check-button {:on-check on-check}])]]))

(defn runner-button-group
  []
  (let [deal-times* (reagent/atom nil)]
    (fn []
      [:div.w-full.h-full.flex.flex-col.justify-end.items-stretch
       [runner-times-button
        {:on-click  #(do (reset! deal-times* 1)
                         (on-request-deal-times 1)),
         :text      "Run ONCE!",
         :selected? (= @deal-times*
                       1),
         :disabled? (some? @deal-times*)}]
       [runner-times-button
        {:on-click  #(do (reset! deal-times* 2)
                         (on-request-deal-times 2)),
         :text      "Run TWICE!",
         :selected? (= @deal-times*
                       2),
         :disabled? (some? @deal-times*)}]])))

(defn join-bet-button-group
  []
  [:div.w-full.h-full.flex.flex-col.justify-end.items-stretch
   [join-bet-button {:on-join-bet on-join-bet}]])

(defn showdown-button-group
  []
  (let [choice* (reagent/atom false)]
    (fn []
      [:div.w-full.h-full.flex.flex-col.justify-end.items-stretch
       [:div.flex-1
        (when-not (= :musk @choice*)
          [reveal-button
           {:on-reveal #(reset! choice* :reveal),
            :clicked   @choice*}])]
       [:div.flex-1
        (when-not (= :reveal @choice*)
          [musk-button
           {:disabled @choice*,
            :on-musk  #(do (reset! choice* :musk)
                           (on-musk))}])]])))


(defn panel
  [{:keys [current-player pots street-bet opts players messages], game-status :status, :as props}]
  (let [{:keys [status stack]} current-player
        buttons-set (get-buttons-set players current-player street-bet)
        pot-val     (transduce (map :value) + 0 pots)]
    ;; container
    [:div.bg-gray-900.md:bg-transparent.md:absolute.md:left-0.md:bottom-0.md:right-0.flex.justify-end.items-stretch.h-36.self-stretch
     ;; right part
     [:div.w-full.sm:w-72.lg:w-96.md:max-w-full.flex.flex-col.justify-end.items-stretch
      ;; message box
      [:div.flex-1.relative
       [:div.absolute.bottom-0.left-0.right-0.p-1
        [message-box messages]]]
      [:div.h-24
       (cond
         (= :player-status/off-seat status)
         [:div.text-md.flex.justify-center.items-center.h-24.text-gray-500
          "Pick a seat to join"]

         (= game-status :game-status/showdown-prepare)
         (when (#{:player-status/acted} status)
           [showdown-button-group])

         (= game-status :game-status/runner-prepare)
         (when (#{:player-status/acted :player-status/all-in} status)
           [runner-button-group])

         (seq buttons-set)
         [bet-button-group
          {:pot-val     (or pot-val 0),
           :opts        opts,
           :bet         (or (last (:bets current-player)) 0),
           :street-bet  (or street-bet 0),
           :buttons-set buttons-set,
           :stack       stack}]

         (= :player-status/wait-for-bb (:status current-player))
         [join-bet-button-group]

         (= :player-status/wait-for-start (:status current-player))
         [:div.text-md.flex.justify-center.items-center.h-24.text-gray-500
          "Waiting for game start"])]]]))
