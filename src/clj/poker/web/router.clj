(ns poker.web.router
  (:require
   [poker.web.handler       :as hdlr]
   [poker.web.ws            :as ws]
   [compojure.core          :refer [ANY GET POST defroutes]]
   [compojure.route         :as route]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params  :refer [wrap-params]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.transit :refer [wrap-transit-params wrap-transit-response]]
   [ring.middleware.gzip    :refer [wrap-gzip]]))

(defroutes routes
  (ANY "/alive" req (hdlr/alive req))
  (GET "/" req (hdlr/game-index-page req))
  (GET "/chsk" req (ws/ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ws/ring-ajax-post req))
  (POST "/auth" req (hdlr/auth req))
  (POST "/signup" req (hdlr/signup req))
  (route/resources "/")
  (route/not-found "<h4>Not found!</h4>"))

(def app
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-transit-params)
      (wrap-transit-response)
      (wrap-anti-forgery)
      (wrap-session)
      (wrap-gzip)))
