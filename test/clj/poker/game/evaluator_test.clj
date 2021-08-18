(ns poker.game.evaluator-test
  (:require
   [poker.game.evaluator :as sut]
   [clojure.test         :as t]))

(t/deftest get-flush-suit-cards
  (t/testing "simple"
    (t/is (= [[:s :a] [:s :q] [:s :t] [:s :9] [:s :8]]
             (sut/get-flush-suit-cards [[:s :a] [:c :5] [:s :q] [:s :9] [:c :6] [:s :t] [:s :8]]))))
  (t/testing "non-exist"
    (t/is (nil? (sut/get-flush-suit-cards [[:s :a] [:c :5] [:h :q] [:s :9] [:c :6] [:s :t]
                                           [:s :8]])))))

(t/deftest evaluate-cards
  (t/testing "royal flush"
    (t/is (= {:category :royal-flush,
              :value    [9 14 13 12 11 10],
              :picks    [[:s :a] [:s :k] [:s :q] [:s :j] [:s :t]]}
             (sut/evaluate-cards [[:s :a] [:s :k] [:s :q] [:s :t] [:s :j] [:s :9] [:s :8]]))))
  (t/testing "straight flush"
    (t/is (= {:category :straight-flush,
              :value    [8 13 12 11 10 9],
              :picks    [[:s :k] [:s :q] [:s :j] [:s :t] [:s :9]]}
             (sut/evaluate-cards [[:s :k] [:s :7] [:s :j] [:s :9] [:s :t] [:s :2] [:s :q]]))))
  (t/testing "four of a kind"
    (t/is (= {:category :four-of-a-kind,
              :value    [7 10 10 10 10 5],
              :picks    [[:s :t] [:d :t] [:h :t] [:c :t] [:s :5]]}
             (sut/evaluate-cards [[:s :t] [:d :3] [:d :t] [:h :t] [:c :t] [:s :5]]))))
  (t/testing "full house"
    (t/is (= {:category :full-house,
              :value    [6 3 3 3 2 2],
              :picks    [[:d :3] [:s :3] [:h :3] [:d :2] [:s :2]]}
             (sut/evaluate-cards [[:d :3] [:s :3] [:h :3] [:d :2] [:s :2] [:s :a] [:d :7]]))))
  (t/testing "flush"
    (t/is
     (= {:category :flush, :value [5 13 7 5 4 3], :picks [[:s :k] [:s :7] [:s :5] [:s :4] [:s :3]]}
        (sut/evaluate-cards [[:d :a] [:s :k] [:s :7] [:s :5] [:s :4] [:s :3] [:d 5]]))))
  (t/testing "straight"
    (t/is (= {:category :straight,
              :value    [4 13 12 11 10 9],
              :picks    [[:d :k] [:d :q] [:s :j] [:d :t] [:d :9]]}
             (sut/evaluate-cards [[:d :k] [:d :q] [:d :t] [:s :j] [:h :5] [:h :2] [:d :9]]))))
  (t/testing "three-of-a-kind"
    (t/is (= {:category :three-of-a-kind,
              :value    [3 13 13 13 14 6],
              :picks    [[:d :k] [:s :k] [:c :k] [:s :a] [:s :6]]}
             (sut/evaluate-cards [[:c :3] [:c :2] [:d :k] [:s :k] [:c :k] [:s :a] [:s :6]]))))
  (t/testing "two-pairs"
    (t/is (= {:category :two-pairs,
              :value    [2 14 14 13 13 6],
              :picks    [[:c :a] [:s :a] [:d :k] [:s :k] [:s :6]]}
             (sut/evaluate-cards [[:c :3] [:c :2] [:d :k] [:s :k] [:c :a] [:s :a] [:s :6]]))))
  (t/testing "pair"
    (t/is
     (=
      {:category :pair, :value [1 14 14 13 12 6], :picks [[:c :a] [:s :a] [:s :k] [:d :q] [:s :6]]}
      (sut/evaluate-cards [[:c :3] [:c :2] [:d :q] [:s :k] [:c :a] [:s :a] [:s :6]]))))
  (t/testing "highcard"
    (t/is (= {:category :highcard,
              :value    [0 14 13 12 10 6],
              :picks    [[:c :a] [:s :k] [:d :q] [:s :t] [:s :6]]}
             (sut/evaluate-cards [[:c :3] [:c :2] [:d :q] [:s :k] [:c :a] [:s :t] [:s :6]])))))
