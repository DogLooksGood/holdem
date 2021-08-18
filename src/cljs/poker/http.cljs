(ns poker.http
  (:require
   [ajax.core     :refer [POST]]
   [re-frame.core :as re-frame]
   [poker.csrf    :refer [?csrf-token]]))

(comment
  ;; usages
  (post! "/alive" {:a 1} nil))

(defn post!
  [api params callback]
  (let [hdlr (when-let [{:keys [success failure]} callback]
               (fn [data]
                 (if (:error data)
                   (do (.debug js/console "failure" data)
                       (when failure
                         (re-frame/dispatch [failure data])))
                   (do (.debug js/console "success " data)
                       (when success
                         (re-frame/dispatch [success data]))))))]
    (POST api
          {:params  params,
           :headers {"X-CSRF-TOKEN" ?csrf-token},
           :handler hdlr})))
