(ns poker.utils-test
  (:require [poker.utils :as sut]
            [clojure.test :as t]))

(t/deftest rotate-by
  (t/testing "empty list" (t/is (= [] (sut/rotate-by odd? []))))
  (t/testing "one item" (t/is (= [1] (sut/rotate-by odd? [1]))))
  (t/testing "one item rotate" (t/is (= [2] (sut/rotate-by odd? [2]))))
  (t/testing "two item" (t/is (= [1 2] (sut/rotate-by odd? [1 2]))))
  (t/testing "two item rotate" (t/is (= [1 2] (sut/rotate-by odd? [2 1]))))
  (t/testing "more items"
    (t/is (= [5 6 7 5 6 7 1 2 3 4] (sut/rotate-by #(= 5 %) [1 2 3 4 5 6 7 5 6 7])))))

(t/deftest rotate (t/is (= [:b :c :a] (sut/rotate 1 [:a :b :c]))))

(t/deftest map-vals (t/is (= {1 1, 2 2} (sut/map-vals inc {1 0, 2 1}))))

(t/deftest keep-vals (t/is (= {:a 1, :b 2} (sut/keep-vals :x {:a {:x 1}, :b {:x 2}, :c {:x nil}}))))
