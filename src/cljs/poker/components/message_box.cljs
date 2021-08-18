(ns poker.components.message-box
  (:require
   [re-frame.core  :as re-frame]
   [reagent.core   :as r]
   [poker.subs.chat]
   [poker.events.chat]
   [clojure.string :as str]))

(defn on-message-box-keydown
  [^js e]
  (when (= "Enter" (.-key e))
    (let [content (-> (.. e -target -value)
                      (str/trim))]
      (when-not (str/blank? content)
        (re-frame/dispatch
         [:chat/post-message content]))
      (set! (.. e -target -value) "")
      (.preventDefault e))))

(defn render-message
  [msg]
  [:div.w-full.flex-wrap.flex
   [:span.font-bold.mr-1
    (if-let [pname (some-> msg
                           :message/sender
                           :player/name)]
      (str pname ": ")
      "Dealer :")]
   [:span
    (-> msg
        :message/content)]])

(defn message-box
  [_msgs]
  (let [state* (r/atom {:status :idle})]
    (fn [msgs]
      (let [expand (= :expand (:status @state*))]
        [:div.w-full.h-100.flex.flex-col.justify-end.items-start.overflow-hidden.no-scrollbar.text-white
         [:div.flex-1.self-stretch.flex.flex-col.justify-end.items-stretch.z-40
          {:class (when expand ["bg-gradient-to-t" "from-gray-800" "to-transparent"])}
          [:div.px-1.message-box-opacity.z-40
           {:class (if expand ["opacity-90"] ["opacity-0"])}
           (for [msg (reverse (drop 3 msgs))]
             ^{:key (:message/id msg)}
             [render-message msg])]
          ^{:key (-> msgs
                     first
                     :message/id)}
          [:div.px-1.message-box-opacity.z-40.relative
           {:class (case (:status @state*)
                     :expand ["opacity-90"]
                     :idle   ["opacity-0" "message-box-fade"]
                     :closed ["opacity-0"])}
           (when (and (= :idle (:status @state*))
                      (seq msgs))
             [:div.absolute.right-0.top-0.w-6.h-4
              {:on-click #(swap! state* assoc :status :closed)}
              "❌"])
           (for [msg (reverse (take 3 msgs))]
             ^{:key (:message/id msg)}
             [render-message msg])]]
         [:div.w-full.mt-2.h-10.message-box-opacity.flex.flex-col.justify-end.items-stretch.relative
          {:class (if expand ["opacity-100"] ["opacity-50"])}
          [:input.h-10.bg-gray-900
           {:type        "text",
            :on-key-down on-message-box-keydown,
            :on-focus    #(do (swap! state* assoc :status :expand) false),
            :on-blur     #(swap! state* assoc :status :idle)}]
          [:div.absolute.right-2.top-0.bottom-0.z-50.text-white.flex.items-center.justify-center.text-2xl
           "💬"]]]))))
