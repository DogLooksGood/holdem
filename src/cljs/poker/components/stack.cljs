(ns poker.components.stack)

;; stack we have 1, 5, 10, 20, 50
(defn split-value
  [value]
  (loop [r   value
         acc []]
    (condp <= r
      10000 (recur (- r 10000) (conj acc 10000))
      5000  (recur (- r 5000) (conj acc 5000))
      2000  (recur (- r 2000) (conj acc 2000))
      1000  (recur (- r 1000) (conj acc 1000))
      500   (recur (- r 500) (conj acc 500))
      200   (recur (- r 200) (conj acc 200))
      100   (recur (- r 100) (conj acc 100))
      50    (recur (- r 50) (conj acc 50))
      20    (recur (- r 20) (conj acc 20))
      10    (recur (- r 10) (conj acc 10))
      5     (recur (- r 5) (conj acc 5))
      1     (recur (- r 1) (conj acc 1))
      acc)))

(def stack-classes
  {1     "bg-pink-400 border border-black",
   5     "bg-purple-400 border border-black",
   10    "bg-indigo-400 border border-black",
   20    "bg-blue-400 border border-black",
   50    "bg-green-400 border border-black",
   100   "bg-yellow-400 border border-black",
   200   "bg-red-400 border border-black",
   500   "bg-blue-700 border border-black",
   1000  "bg-green-400 border border-black",
   2000  "bg-yellow-500 border border-black",
   5000  "bg-red-700 border border-black",
   10000 "bg-black border border-black"})

(defn stack
  [{:keys [value]}]
  (if (> value 50000)
    [:div.bg-red-800.border-2.border-red-500.rounded-lg.text-yellow-500.h-8.w-20.flex.items-center.justify-center.font-bold
     value]
    (let [values (split-value value)]
      [:div.flex.flex-col.items-center
       (for [[idx v] (map-indexed vector (reverse values))]
         [:div.h-1.w-5.relative {:key idx}
          [:div.absolute.rounded-full.w-4.h-4.flex.justify-center.items-center
           {:class (get stack-classes v),
            :style {:z-index (str (- 20 idx))}}
           [:div.rounded-full.w-full.h-full.border-2.border-dashed.border-gray-100.flex.justify-center.items-center
            [:div.w-1.h-1.left-1.top-1.bg-gray-100.rounded-full]]]])
       [:div.mt-4.text-sm.text-gray-300 value]])))
