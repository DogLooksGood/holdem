(ns poker.mobile)

(defn mobile-device?
  []
  (some?
   (re-find #"(?i)iPhone|iPad|iPod|Android" (.-userAgent js/navigator))))

(defn firefox?
  []
  (some?
   (re-find #"Firefox" (.-userAgent js/navigator))))

(defn setup-for-mobile
  []
  (let [vh (* 0.01 (.-innerHeight js/window))]
    (.setProperty (.. js/document -documentElement -style) "--vh" (str vh "px"))))

(defonce resize-hook (.addEventListener js/window "resize" setup-for-mobile))
