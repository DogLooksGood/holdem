(ns poker.components.help
  (:require
   [poker.components.card :refer [card-in-help]]))

(defn help
  [{:keys [on-close on-toggle]}]
  [:<>
   [:div.flex.justify-center.px-4.py-1.text-lg.font-bold
    [:pre.cursor-pointer {:on-click on-toggle} "History "]
    [:pre "/"]
    [:pre.text-yellow-400 " Help"]
    [:div.absolute.right-4.top-4.w-8.h-8.bg-red-700.rounded-full.flex.justify-center.items-center.cursor-pointer
     {:on-click on-close}
     "✖"]]
   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Royal Flush"]
    [:div.flex
     [card-in-help [:s :a] false]
     [card-in-help [:s :k] false]
     [card-in-help [:s :q] false]
     [card-in-help [:s :j] false]
     [card-in-help [:s :t] false]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Straight Flush"]
    [:div.flex
     [card-in-help [:s :t] false]
     [card-in-help [:s :9] false]
     [card-in-help [:s :8] false]
     [card-in-help [:s :7] false]
     [card-in-help [:s :6] false]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Four Of A Kind"]
    [:div.flex
     [card-in-help [:s :3] false]
     [card-in-help [:h :3] false]
     [card-in-help [:c :3] false]
     [card-in-help [:d :3] false]
     [card-in-help [:s :j] true]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Full House"]
    [:div.flex
     [card-in-help [:s :5] false]
     [card-in-help [:h :5] false]
     [card-in-help [:c :5] false]
     [card-in-help [:d :k] false]
     [card-in-help [:s :k] false]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Flush"]
    [:div.flex
     [card-in-help [:h :a] false]
     [card-in-help [:h :q] false]
     [card-in-help [:h :5] false]
     [card-in-help [:h :4] false]
     [card-in-help [:h :2] false]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Straight"]
    [:div.flex
     [card-in-help [:h :8] false]
     [card-in-help [:h :7] false]
     [card-in-help [:c :6] false]
     [card-in-help [:h :5] false]
     [card-in-help [:s :4] false]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Three Of A Kind"]
    [:div.flex
     [card-in-help [:c :7] false]
     [card-in-help [:d :7] false]
     [card-in-help [:s :7] false]
     [card-in-help [:h :a] true]
     [card-in-help [:s :3] true]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Two Pair"]
    [:div.flex
     [card-in-help [:c :a] false]
     [card-in-help [:d :a] false]
     [card-in-help [:s :k] false]
     [card-in-help [:h :k] false]
     [card-in-help [:s :3] true]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "One Pair"]
    [:div.flex
     [card-in-help [:c :6] false]
     [card-in-help [:d :6] false]
     [card-in-help [:s :k] true]
     [card-in-help [:h :q] true]
     [card-in-help [:s :3] true]]]

   [:div.flex.justify-between.px-4.py-1.bg-gray-800.m-1.py-2.items-center
    [:div "Highcard"]
    [:div.flex
     [card-in-help [:c :a] false]
     [card-in-help [:s :k] true]
     [card-in-help [:s :j] true]
     [card-in-help [:d :6] true]
     [card-in-help [:s :3] true]]]])
