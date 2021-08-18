(ns poker.app
  (:require
   ["react-dom"   :as react-dom]
   [reagent.core  :as r]
   [re-frame.core :as re-frame]
   [reagent.dom   :as rdom]
   [poker.router  :as router]
   [poker.logger  :as log]
   [poker.mobile :refer [setup-for-mobile]]
   [poker.events.router]
   [poker.events.account]
   [poker.events.api]
   [poker.events.store]
   [poker.subs.router]))

(defn mount
  []
  (rdom/render [router/router-component]
               (.getElementById js/document "app")))

(defn hydrate
  []
  (react-dom/hydrate (r/as-element [router/router-component])
                     (.getElementById js/document "app")))

(defn -main
  []
  (setup-for-mobile)
  (log/info "init routes")
  (router/init-routes!)
  (log/info "try auth token")
  (re-frame/dispatch-sync [:account/auth-token])
  (log/info "initialize router")
  (re-frame/dispatch-sync [:router/initialize-router])
  (re-frame/dispatch-sync [:router/push-state :index])
  (log/info "render")
  (if (.getElementById js/document "ssr-enabled")
    (hydrate)
    (mount)))

(defn after-load [] (mount))
