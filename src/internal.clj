(ns internal
  (:require
   [helpers :refer [merge-maps]]))

(defn wrap-internal [queue-update value])

(defn q-internal [initial builder]
  ^{::type ::internal}
  (fn [state updates context queue-update]
    (let [internal (merge-maps initial (:internal state) (or (get [] updates) {}))
          wrapped-internal (->> internal
                                (map (fn [[key value]]
                                       [key (wrap-internal queue-update value)]))
                                (into {}))]
      ((builder internal) (assoc state :internal internal)
                          updates
                          context
                          queue-update))))
