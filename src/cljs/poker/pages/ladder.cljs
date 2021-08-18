(ns poker.pages.ladder
  "Page for ladder."
  (:require
   [re-frame.core :as re-frame]
   [poker.events.ladder]
   [poker.subs.ladder]
   [poker.utils :as u]))

(defn ladder-page-init
  "Query for all games."
  []
  (re-frame/dispatch [:ladder/list-leaderboard]))

(defn ladder-page
  []
  (let [leaderboard* (re-frame/subscribe [:ladder/leaderboard])]
    (fn []
      (let [leaderboard @leaderboard*]
        [:div.h-screen.w-screen.flex.flex-col.justify-start.items-stretch.text-gray-900.p-4
         [:button.absolute.left-0.top-0.p-4
          {:on-click #(re-frame/dispatch [:router/push-state :lobby])}
          "Lobby"]
         [:button.absolute.right-0.top-0.p-4
          {:on-click #(re-frame/dispatch [:ladder/list-leaderboard])}
          "Refresh"]
         [:div.text-2xl.mt-8.text-center "Ladder Leaderboard"]
         (for [[idx
                {:player/keys [name avatar],
                 :ladder/keys [hands buyin returns score]}]

               (map-indexed vector leaderboard)]
           ^{:key name}
           [:div.border.bg-gray-200.border-gray-500.px-1.py-1.my-1.flex.justify-start.items-center.relative

            ;; left
            [:div.bg-gray-700.font-bold.rounded-full.w-14.h-14.flex.place-content-center.text-4xl.mr-1
             avatar]

            ;; middle
            [:div.flex-1.flex.items-center.justify-between.lg:flex-row.flex-col
             [:div.text-lg.flex-1 name]
             [:div.font-bold.text-white.text-sm.px-1.rounded-sm.w-20.text-center
              {:class (if (pos? score) ["bg-green-700"] ["bg-red-700"])}
              score]]

            ;; right
            [:div.flex-1.flex.flex-col.items-stretch.px-4.text-sm
             {:class ["w-1/3"]}
             [:div.flex.justify-between.text-blue-900
              [:div "Hands:"] [:div hands]]
             [:div.flex.justify-between.text-red-900
              [:div "Buyin:"] [:div (u/format-stack-value buyin)]]
             [:div.flex.justify-between.text-green-900
              [:div "Returns:"] [:div (u/format-stack-value returns)]]]])]))))
