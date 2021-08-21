(ns poker.components.button
  (:require
   [reagent.core   :as reagent]
   [poker.logger   :as log]
   [clojure.string :as str]))

(defn uniq-by-value
  [pairs]
  (reduce (fn [acc x]
            (if (= (first (peek acc)) (first x))
              acc
              (conj acc x)))
          []
          pairs))

;; TODO should use min-raise here
(defn list-raise-values
  "list following values when pot is empty:
  - 2 * street-bet (mini raise)
  - pot + street-bet
  - 1.5 * pot
  - 2 * pot
  - 3 * pot
  - all-in (stack) "
  [min-raise sb bb bet street-bet pot stack]
  (.log js/console [min-raise sb bb bet street-bet pot stack])
  (let [min (- (+ street-bet (or min-raise street-bet)) bet)]
    (if (= pot (+ sb bb))
      (let [values [[(* 1 bb) "1BB"]
                    [(* 2 bb) "2BB"]
                    [(* 3 bb) "3BB"]
                    [(* 4 bb) "4BB"]
                    [(* 5 bb) "5BB"]
                    [stack "ALL-IN"]]]
        (->> values
             (filter #(or (<= min (first %) stack)
                          (= (first %) stack)))
             (reverse)
             (uniq-by-value)))
      (let [values [[stack "ALL-IN"]
                    [min "MIN"]
                    [(* 2 pot) "2X"]
                    [(int (* 1.5 pot)) "3/2X"]]]
        (->> values
             ;; No raise greater than stack.
             (filter #(or (<= (or min bb) (first %) stack)
                          (= (first %) stack)))
             (sort-by first)
             (reverse)
             (uniq-by-value))))))

(defn list-bet-values
  "list following values:
  - 1/3 pot
  - 1/2 pot
  - 2/3 pot
  - pot
  - 3/2 pot
  - 2 pot
  - all-in (stack) "
  [pot stack bb]
  (let [values [[stack "ALL-IN"]
                [(int (/ pot 3)) "1/3X"]
                [(int (/ pot 2)) "1/2X"]
                [(int (/ (* 2 pot) 3)) "2/3X"]
                [pot "1X"]
                [(int (/ (* 3 pot) 2)) "3/2X"]
                [(* pot 2) "2X"]]]
    (->> values
         (filter #(<= bb (first %) stack))
         (sort-by first)
         (reverse)
         (uniq-by-value))))

(def idx->raise-value-cls
  {0 ["bg-red-700" "hover:bg-red-800"],
   1 ["bg-red-600" "hover:bg-red-700"],
   2 ["bg-red-500" "hover:bg-red-600"],
   3 ["bg-red-400" "hover:bg-red-500"],
   4 ["bg-red-300" "hover:bg-red-400"],
   5 ["bg-red-200" "hover:bg-red-300"],
   6 ["bg-red-100" "hover:bg-red-200"]})

(defn bet-button
  [{:keys [stack pot on-bet bb]}]
  (let [state* (reagent/atom {:status :idle,
                              :input  nil})
        values (list-bet-values pot stack bb)]
    (fn []
      (let [{:keys [input status]} @state*
            on-change     (fn [e]
                            (let [v (aget e "target" "value")]
                              (log/info v)
                              (if (str/blank? v)
                                (swap! state* assoc :input nil)
                                (swap! state* assoc :input (int v)))))
            set-idle      (fn [] (swap! state* assoc :status :idle))
            expand-or-bet (fn []
                            (if (and input (<= bb (int input) stack))
                              (on-bet (int input))
                              (swap! state* assoc :status :expand)))]
        (if (seq values)
          (case status
            :idle
            [:div.flex-1.flex
             [:div.flex-1.h-10.m-1
              [:input.w-full.h-full.bg-gray-700.text-white.border-gray-900.text-center
               {:type        "number",
                :value       input,
                :on-change   on-change,
                :placeholder (str bb " ~ " stack),
                :on-key-down #(when (= "Enter" (aget % "key")) (expand-or-bet))}]]
             [:div.flex-1.m-1.border.border-red-700.bg-red-800.hover:bg-red-500.shadow-xl.text-gray-200.text-md.rounded-sm.h-10.flex.items-center.justify-center.cursor-pointer
              {:on-click expand-or-bet}
              "Bet"]]

            :expand
            [:div.relative.m-1.self-stretch.h-10 {:on-mouse-leave set-idle}
             [:div.absolute.border.border-black.bottom-0.flex.flex-col.w-full.z-50
              (for [[idx [v lbl]] (map-indexed vector values)]
                (let [cls (get idx->raise-value-cls idx)]
                  ^{:key idx}
                  [:div.justify-between.items-center.flex.px-2.h-10.cursor-pointer
                   {:class    cls,
                    :on-click #(do (set-idle)
                                   (when on-bet
                                     (on-bet
                                      v)))}
                   [:div.font-bold v]
                   [:div.text-gray-800.text-2xs lbl]]))
              [:div.justify-between.items-center.flex.px-2.h-10.cursor-pointer.bg-transparent.text-gray-200
               {:on-click set-idle}
               [:div.font-bold "Cancel"]]]])
          [:div])))))

(defn raise-button
  [{:keys [stack pot min-raise street-bet on-raise sb bb bet]}]
  (let [state* (reagent/atom {:status :idle,
                              :input  nil})
        values (list-raise-values min-raise sb bb bet street-bet pot stack)
        min    (- (+ street-bet (or min-raise street-bet)) bet)]
    (fn []
      (let [{:keys [input status]} @state*
            on-change     (fn [e]
                            (let [v (aget e "target" "value")]
                              (log/info v)
                              (if (str/blank? v)
                                (swap! state* assoc :input nil)
                                (swap! state* assoc :input (int v)))))
            set-idle      (fn [] (swap! state* assoc :status :idle))
            expand-or-bet (fn []
                            (if (and input (<= (or min-raise street-bet) (int input) stack))
                              (on-raise (int input))
                              (swap! state* assoc :status :expand)))]
        (if (seq values)
          (case status
            :idle
            [:div.flex-1.flex
             [:div.flex-1.h-10.m-1
              [:input.w-full.h-full.bg-gray-700.text-white.border-gray-900.text-center
               {:type        "number",
                :value       input,
                :on-change   on-change,
                :placeholder (str min " ~ " stack),
                :on-key-down #(when (= "Enter" (aget % "key")) (expand-or-bet))}]]
             [:div.flex-1.m-1.border.border-red-700.bg-red-800.hover:bg-red-500.shadow-xl.text-gray-200.text-md.rounded-sm.h-10.flex.items-center.justify-center.cursor-pointer
              {:on-click expand-or-bet}
              "Raise"]]

            :expand
            [:div.relative.m-1.self-stretch.h-10 {:on-mouse-leave set-idle}
             [:div.absolute.border.border-black.bottom-0.flex.flex-col.w-full.z-50
              (for [[idx [v lbl]] (map-indexed vector values)]
                (let [cls (get idx->raise-value-cls idx)]
                  ^{:key idx}
                  [:div.justify-between.items-center.flex.px-2.h-10.cursor-pointer
                   {:class    cls,
                    :on-click #(do (set-idle)
                                   (when on-raise
                                     (on-raise
                                      v)))}
                   [:div.font-bold v]
                   [:div.text-gray-800.text-2xs lbl]]))
              [:div.justify-between.items-center.flex.px-2.h-10.cursor-pointer.bg-transparent.text-gray-200
               {:on-click set-idle}
               [:div.font-bold "Cancel"]]]])
          [:div])))))

(defn join-bet-button
  [{:keys [on-join-bet]}]
  [:div.border.border-green-600.bg-green-700.hover:bg-green-500.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
   {:on-click on-join-bet}
   "Blind Bet to join"])

(defn check-button
  [{:keys [on-check]}]
  [:div.border.border-blue-600.bg-blue-700.hover:bg-blue-500.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
   {:on-click on-check}
   "Check"])

(defn call-button
  [{:keys [on-call street-bet bet stack]}]
  (let [value-to-call (- street-bet bet)]
    (cond
      (>= value-to-call stack)
      [:div.border.border-yellow-600.bg-yellow-700.hover:bg-yellow-500.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
       {:on-click on-call}
       "Call ALL-IN"]

      (zero? value-to-call)
      [:div.border.border-green-600.bg-green-700.hover:bg-green-500.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
       {:on-click on-call}
       "Call"]

      :else
      [:div.border.border-green-600.bg-green-700.hover:bg-green-500.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
       {:on-click on-call}
       "Call"
       [:span.font-bold.ml-1 (str value-to-call)]])))

(defn fold-button
  [{:keys [on-fold]}]
  [:div.border.border-gray-500.bg-gray-700.hover:bg-gray-600.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
   {:on-click on-fold}
   "Fold"])

;; We don't really send reveal event to server
(defn reveal-button
  [{:keys [on-reveal clicked]}]
  (reagent/with-let
   [cnt* (reagent/atom 2)
    interval (js/setInterval #(swap! cnt* (fn [v] (cond-> v pos? dec))) 1000)]
   [:div.border.border-green-500.bg-green-700.hover:bg-green-600.shadow-xl.text-green-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center
    {:on-click on-reveal,
     :class    (if clicked ["diasbled" "cursor-not-allowed"] ["cursor-pointer"]),
     :diasbled (some? clicked)}
    (if clicked
      "Reveal"
      (str
       "Reveal ("
       @cnt*
       ")"))]
   (finally
    (js/clearInterval interval))))

(defn musk-button
  [{:keys [on-musk disabled]}]
  [:div.border.border-gray-500.bg-gray-700.hover:bg-gray-600.shadow-xl.text-gray-200.text-md.rounded-sm.m-1.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
   {:on-click on-musk,
    :disabled disabled}
   "Musk"])

(defn runner-times-button
  [{:keys [on-click text selected? disabled?]}]
  (cond
    selected?
    [:div.border.border-green-500.bg-green-700.shadow-xl.text-green-200.text-md.rounded-sm.m-2.h-10.flex-1.flex.items-center.justify-center.cursor-not-allowed
     text]
    disabled?
    [:div.border.border-gray-500.bg-gray-700.shadow-xl.text-gray-200.text-md.rounded-sm.m-2.h-10.flex-1.flex.items-center.justify-center.cursor-not-allowed
     text]
    :else
    [:div.border.border-green-500.bg-green-700.hover:bg-green-600.shadow-xl.text-green-200.text-md.rounded-sm.m-2.h-10.flex-1.flex.items-center.justify-center.cursor-pointer
     {:on-click on-click}
     text]))

(defn button
  [{:keys [text onclick style]}]
  [:div.border.border-red-700.bg-red-800.shadow-xl.text-gray-200.text-md.rounded-sm.m-2.h-10.flex.items-center.justify-center
   {:class [(case style
              :danger "border-red-700 bg-red-800 hover:bg-red-900 text-gray-200"
              :info   "border-yellow-500 bg-yellow-600 hover:bg-yellow-700 text-gray-200"
              "border-green-600 bg-green-700 text-gray-200")]}
   text])
