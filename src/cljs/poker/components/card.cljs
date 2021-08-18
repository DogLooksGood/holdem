(ns poker.components.card
  (:require
   [clojure.string :as str]
   [goog.string    :as gstr]))

(def kind->order
  {:2 2, :3 3, :4 4, :5 5, :6 6, :7 7, :8 8, :9 9, :t 10, :j 11, :q 12, :k 13, :a 14})

(defn sort-cards
  [cards]
  (sort-by (comp kind->order second) > cards))

(defn throw-invalid-card-suit!
  [card]
  (throw (ex-info "Invalid card suit" {:card card})))

(defn card-placeholder
  []
  [:div.w-16.h-24])

(defn parse-suit-str
  [suit]
  (case suit
    :s (gstr/unescapeEntities "&#9824;")
    :c (gstr/unescapeEntities "&#9827;")
    :h (gstr/unescapeEntities "&#9829;")
    :d (gstr/unescapeEntities "&#9830;")))

(defn parse-kind-str
  [kind]
  (if (= kind :t)
    "10"
    (str/upper-case (name kind))))

(defn card-in-help
  [[suit kind] dim?]
  (let [suit-str (parse-suit-str suit)
        kind-str (parse-kind-str kind)
        clr-cls
        (case suit
          (:s :c) "text-black"
          (:h :d) "text-red-800")]
    [:span.w-6.h-8.border.border-black.flex.flex-row.justify-around
     {:class [clr-cls (if dim? "bg-gray-400" "bg-white")]}
     [:span.text-md.self-start.text-xs kind-str]
     [:span.text-md.self-end.text-sm suit-str]]))

(defn card-in-message
  [[suit kind] highlight?]
  (let [suit-str (parse-suit-str suit)
        kind-str (parse-kind-str kind)
        clr-cls  (case suit
                   (:s :c) "text-black"
                   (:h :d) "text-red-800")]
    [:span.inline-block.w-8.py-1.border.border-black.text-center
     {:class [clr-cls (if highlight? "bg-yellow-300" "bg-white")]}
     [:span.text-xs kind-str]
     [:span.text-sm suit-str]]))

(defn card
  [[suit kind]]
  (let [suit-str (parse-suit-str suit)
        kind-str (parse-kind-str kind)
        clr-cls  (case suit
                   (:s :c) "text-black"
                   (:h :d) "text-red-800")]
    [:div.w-16.h-24.p-2.border.border-black.rounded-md.shadow-xl.bg-white.flex.flex-col.justify-around
     {:class clr-cls}
     [:div.self-start kind-str]
     [:div.self-center {:style {:font-size "32px"}} suit-str]
     [:div.self-end.rotate-180 kind-str]]))

(defn card-back
  []
  [:div.w-16.h-24.p-2.border.border-black.rounded-md.shadow-xl.bg-white.flex.flex-col.justify-center.items-center.bg-blue-800
   {:style {:font-size "3rem"}}
   "🐦"])
