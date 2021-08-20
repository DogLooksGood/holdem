(ns poker.game.evaluator
  (:require
   [clojure.set :as set]))

(def ->suit first)
(def ->kind second)

(def kind->order
  {:2 2, :3 3, :4 4, :5 5, :6 6, :7 7, :8 8, :9 9, :t 10, :j 11, :q 12, :k 13, :a 14})

(def category->order
  {:royal-flush     9,
   :straight-flush  8,
   :four-of-a-kind  7,
   :full-house      6,
   :flush           5,
   :straight        4,
   :three-of-a-kind 3,
   :two-pairs       2,
   :pair            1,
   :highcard        0})

(defn sort-by-kinds
  [cards]
  (->> cards
       (sort-by (comp kind->order ->kind))
       (reverse)))

(defn get-sorted-cards
  [cards]
  (->> cards
       (group-by ->kind)
       (sort-by (juxt (comp count val) (comp kind->order key)))
       (reverse)
       (mapcat val)))

(defn get-flush-suit-cards
  [cards]
  (some->> cards
           (group-by ->suit)
           (filter #(-> %
                        val
                        count
                        (>= 5)))
           first
           val
           (sort-by (comp - kind->order ->kind))))

(defn list-straights
  [cards]
  (let [order->cards (->> cards
                          (group-by #(-> %
                                         ->kind
                                         kind->order)))]
    (->> (conj (mapv #(range % (- % 5) -1) (range 14 2 -1)) [5 4 3 2 14])
         (mapcat (fn [[a b c d e]]
                   (for [ca (order->cards a)
                         cb (order->cards b)
                         cc (order->cards c)
                         cd (order->cards d)
                         ce (order->cards e)]
                     (filterv identity [ca cb cc cd ce]))))
         (filter #(= 5 (count %))))))

(defn- with-value
  [{:keys [category picks], :as m}]
  (let [value (reduce (fn [acc p] (conj acc (kind->order (->kind p))))
                      [(category->order category)]
                      picks)]
    (assoc m :value value)))

(defn evaluate-cards
  "Return the strength of cards"
  [cards]
  (let [sorted-cards     (get-sorted-cards cards)
        sorted-kinds     (mapv ->kind sorted-cards)
        flush-suit-cards (get-flush-suit-cards cards)
        straights        (list-straights cards)]
    (with-value
     (or
      ;; royal flush
      (let [picks (->> (for [suit [:s :h :d :c]] (for [kind [:a :k :q :j :t]] [suit kind]))
                       (filter #(= 5 (count (set/intersection (set cards) (set %)))))
                       first)]
        (when picks
          {:category :royal-flush,
           :picks    picks}))
      ;; straight flush
      (when-let [picks (->> straights
                            (filter #(= 5
                                        (count (set/intersection (set flush-suit-cards) (set %)))))
                            first)]
        {:category :straight-flush,
         :picks    picks})
      ;; four of a kind
      (when (apply = (take 4 sorted-kinds))
        {:category :four-of-a-kind,
         :picks    (vec (take 5 sorted-cards))})
      ;; full house
      (when (and (apply = (map sorted-kinds [0 1 2])) (apply = (map sorted-kinds [3 4])))
        {:category :full-house,
         :picks    (vec (take 5 sorted-cards))})
      ;; flush
      (when flush-suit-cards
        {:category :flush,
         :picks    flush-suit-cards})
      ;; straight
      (when-let [picks (first straights)]
        {:category :straight,
         :picks    picks})
      ;; three of a kind
      (when (apply = (map sorted-kinds [0 1 2]))
        {:category :three-of-a-kind,
         :picks    (vec (take 5 sorted-cards))})
      ;; two pairs
      (when (and (apply = (map sorted-kinds [0 1])) (apply = (map sorted-kinds [2 3])))

        {:category :two-pairs,
         :picks    (conj (vec (take 4 sorted-cards))
                         (first (sort-by-kinds (drop 4 sorted-cards))))})
      ;; pair
      (when (apply = (map sorted-kinds [0 1]))
        {:category :pair,
         :picks    (vec (take 5 sorted-cards))})
      ;; highcard
      {:category :highcard,
       :picks    (vec (take 5 sorted-cards))}))))
