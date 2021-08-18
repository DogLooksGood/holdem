(ns poker.events.router
  (:require
   [re-frame.core        :as re-frame]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

(re-frame/reg-fx :router/push-state
  (fn [route]
    (apply rfe/push-state route)))

(re-frame/reg-event-db :router/initialize-router
  (fn [db _] (assoc db :current-route nil)))

(re-frame/reg-event-fx :router/push-state
  (fn [_ [_ & route]] {:router/push-state route}))

(re-frame/reg-fx :router/new-game-window
  (fn [{:keys [game-id]}]
    (.open
     js/window
     (str "#/game/" game-id)
     (str "Game@" game-id)
     "directories=0,titlebar=0,toolbar=0,location=0,status=0,menubar=0,scrollbars=no,resizable=no,width=400,height=350")))

(re-frame/reg-event-db :router/navigated
  (fn [db [_ new-match]]
    (let [old-match   (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match)
                                             new-match)]
      (assoc db
             :current-route
             (assoc new-match :controllers controllers)))))
