(ns poker.components.history-popup
  (:require
   [re-frame.core         :as re-frame]
   [clojure.string        :as str]
   [poker.subs.history]
   [poker.events.history]
   [poker.components.card :refer [card-in-message]]
   [poker.logger          :as log]))

(defn format-picks
  [picks hole-cards]
  (let [hole-cards-set (set hole-cards)]
    [:span
     (for [p picks]
       ^{:key p}
       [card-in-message p (hole-cards-set p)])]))

(defn format-kind
  [kind]
  (case kind
    :a "ace"
    :k "king"
    :q "queen"
    :j "jack"
    :t "ten"
    :9 "nine"
    :8 "eight"
    :7 "seven"
    :6 "six"
    :5 "five"
    :4 "four"
    :3 "three"
    :2 "deuce"))

(defn format-kind-plural
  [kind]
  (case kind
    :a "aces"
    :k "kings"
    :q "queens"
    :j "jacks"
    :t "tens"
    :9 "nines"
    :8 "eights"
    :7 "sevens"
    :6 "sixes"
    :5 "fives"
    :4 "fours"
    :3 "threes"
    :2 "deuces"))

(defn format-category
  [category picks]
  (let [ks  (mapv (comp format-kind second) picks)
        kss (mapv (comp format-kind-plural second) picks)]
    (case category
      :royal-flush     "royal flush"
      :straight-flush  (str (ks 0) "-high straight flush")
      :four-of-a-kind  (str "four of a kind, " (kss 0))
      :full-house      (str (kss 0) " full of " (kss 4))
      :flush           (str (ks 0) "-high flush")
      :straight        (str (ks 0) "-high straight")
      :three-of-a-kind (str "three of a kind, " (kss 0))
      :two-pairs       (str "two pair, " (kss 0) " and " (kss 2))
      :pair            (str "one pair, " (kss 0))
      :highcard        (str (ks 0) "-" (ks 1) "-high")
      (log/error "No such category" {:category category}))))

(defn history-popup
  [{:keys [on-close on-toggle]}]
  (let [records* (re-frame/subscribe [:history/records])]
    (fn []
      (let [records @records*]
        [:<>
         [:div.flex.justify-center.px-4.py-1.text-lg.font-bold
          [:pre.text-yellow-400 "History "]
          [:pre "/"]
          [:pre.cursor-pointer {:on-click on-toggle} " Help"]
          [:div.absolute.right-4.top-4.w-8.h-8.bg-red-700.rounded-full.flex.justify-center.items-center.cursor-pointer
           {:on-click on-close}
           "✖"]]
         [:div.flex.flex-col.items-stretch
          (for [[idx {:keys [showdowns last-player]}] (map-indexed vector records)]
            ^{:key (str "record-" idx)}
            [:div.m-1.py-4.px-2.bg-gray-800.text-center
             (if showdowns
               (for [[idx {:keys [player-id award category picks hole-cards]}] (map-indexed
                                                                                vector
                                                                                showdowns)]
                 ^{:key (str "showdown-" idx)}
                 (if award
                   [:p
                    [:span.font-bold.mr-2 (:player/name player-id)]
                    [:span.text-red-400 (str "+" award)]
                    [:br]
                    [:span (format-category category picks)]
                    [:br]
                    [:span (format-picks picks hole-cards)]]
                   [:p
                    [:span.font-bold (:player/name player-id)]
                    [:br]
                    [:span (format-category category picks)]
                    [:br]
                    [:span (format-picks picks hole-cards)]]))
               (let [{:keys [player-id award street]} last-player]
                 [:p
                  [:span.font-bold.mr-2 (:player/name player-id)]
                  [:span.text-red-400 (str "+" award)]
                  [:br]
                  "All other players fold at"
                  [:span.font-bold.ml-2 (str/upper-case street)]]))])]]))))
