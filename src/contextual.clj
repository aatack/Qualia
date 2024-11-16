(ns contextual
  (:require
   [helpers :refer [merge-maps]]))

(defn q-provide [values entity]
  ^{::type ::contextual}
  (fn [state updates context queue-update]
    (entity state
            updates
            (merge-maps context values)
            queue-update)))

(defn q-consume [keys builder]
  ^{::type ::consume}
  (fn [state updates context queue-update]
    (let [values (into {} (map (fn [key] [key (context key)]) keys))]
      (update ((builder values) state updates context queue-update)
              :contextual
              (fn [contextual] (merge-maps (or contextual {}) values))))))
