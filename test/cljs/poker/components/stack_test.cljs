(ns poker.components.stack-test
  (:require [poker.components.stack :as sut]
            [cljs.test :as t :include-macros true]
            [nubank.workspaces.core :as ws]))

(ws/deftest split-value
  (t/is (= [200 100]
           (sut/split-value 300)))

  (t/is (= [5000 5000 200 20 20 1 1 1]
           (sut/split-value 10243))))
