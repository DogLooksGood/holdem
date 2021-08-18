(ns poker.utils)

#?(:cljs
     (defn format-stack-value
       [v]
       (cond
         (> (.abs js/Math v) 1000000)
         (str (.toFixed (/ v 1000000) 1) "M")

         (> (.abs js/Math v) 1000)
         (str (.toFixed (/ v 1000) 1) "K")

         :else
         (str v))))

(defn rotate-by
  [pred xs]
  (loop [rotated []
         remain  xs]
    (if (empty? remain)
      rotated
      (if (pred (first remain))
        (concat remain rotated)
        (recur (conj rotated (first remain)) (next remain))))))

(def game-status->street
  {:game-status/preflop :preflop,
   :game-status/flop    :flop,
   :game-status/turn    :turn,
   :game-status/river   :river})

(defn rotate
  [n xs]
  (cond (pos-int? n) (concat (drop n xs) (take n xs))
        (neg-int? n) (concat (take-last (- n) xs) (drop-last (- n) xs))
        :else        xs))

(defn map-vals
  [f m]
  (into {}
        (map (fn [[k v]] [k (f v)]))
        m))

(defn keep-vals
  [f m]
  (into {}
        (keep
         (fn [[k v]]
           (when-let [v (f v)] [k v])))
        m))

(defn filter-vals
  [f m]
  (into {}
        (keep
         (fn [[k v]]
           (when (f v) [k v])))
        m))
