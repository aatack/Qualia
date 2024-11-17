(ns builders.contextual
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
              (fn [current] (merge-maps (or current {}) values))))))

(comment
  (require '[builders.literal :refer [q-literal]])

  (assert ;; Provided values are consumed correctly, and reported
   (= {:contextual {:x 1} :value 1}
      ((q-provide
        {:x 1}
        (q-consume [:x] (fn [values] (q-literal (:x values)))))
       {} {} {} (fn [])))))
