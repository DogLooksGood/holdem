(ns poker.ssr
  (:require [poker.pages.index :refer [index-page]]
            [reagent.dom.server :as d]))

(defn -main []
  (print (d/render-to-string [index-page])))
