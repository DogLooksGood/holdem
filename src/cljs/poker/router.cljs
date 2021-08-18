(ns poker.router
  "Frontend router."
  (:require
   [re-frame.core        :as re-frame]
   [reitit.frontend      :as rf]
   [reitit.frontend.easy :as rfe]
   [poker.pages.index    :refer [index-page]]
   [poker.pages.game     :refer [game-page on-get-current-state]]
   [poker.pages.lobby    :refer [lobby-page lobby-page-init]]
   [poker.pages.ladder   :refer [ladder-page ladder-page-init]]))

(defn on-navigate
  [new-match]
  (when new-match (re-frame/dispatch [:router/navigated new-match])))

(def routes
  [["/" {:name :index, :render index-page, :controllers []}]
   ["/ladder" {:name :ladder, :render (fn [] [ladder-page]) :controllers [{:start ladder-page-init}]}]
   ["/game/:game-id"
    {:name        :game,
     :render      (fn [params] [game-page params]),
     :controllers [{:parameters {:path [:game-id]},
                    :start      (fn [{:keys [path]}]
                                  (on-get-current-state path))}]}]
   ["/lobby"
    {:name :lobby, :render (fn [] [lobby-page]), :controllers [{:start lobby-page-init}]}]])

(def router (rf/router routes {:data {}}))

(defn init-routes!
  []
  (rfe/start! router on-navigate {:use-fragment true}))

(defn router-component
  []
  (let [route  @(re-frame/subscribe [:router/current-route])
        render (or (get-in route [:data :render])
                   index-page)]
    [render (merge (:path-params route) (:query-params route))]))
