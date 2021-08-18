(ns poker.logger)

(defn info [& args]
  (when goog.DEBUG
    (apply js/console.log args)))

(defn debug [& args]
  (when goog.DEBUG
    (apply js/console.debug args)))

(defn error [& args]
  (when goog.DEBUG
    (apply js/console.error args)))
