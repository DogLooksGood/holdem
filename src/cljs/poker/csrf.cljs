(ns poker.csrf)

(def ?csrf-token
  (some->
    (.getElementById js/document "sente-csrf-token")
    (.getAttribute "data-csrf-token")))
