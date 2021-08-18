(ns poker.events.store
  (:require [akiroz.re-frame.storage :refer [reg-co-fx!]]))

(reg-co-fx! :holdem
  {:fx :store
   :cofx :store})
