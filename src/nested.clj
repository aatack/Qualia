(ns nested
  (:require
   [helpers :refer [merge-maps]]))

(defn- wrap-queue-update [queue-update key])

(defn- filter-updates [updates key]
  (->> updates (filter (fn [[item-key _]] (= (first item-key) key))) (into {})))

(defn q-nested [entities builder]
  ^{::type ::nested}
  (fn [state updates context queue-update]
    (let [updated-entities
          (into {}
                (map (fn [[key entity]]
                       (entity (or (-> state :nested (get key)) {})
                               (filter-updates updates key)
                               context
                               (wrap-queue-update queue-update key)))
                     entities))

          resulting-state
          ((builder updated-entities)
           (update state
                   :internal
                   (fn [current]
                     (reduce dissoc
                             current
                             (keys updated-entities))))
           (into {}
                 (filter (fn [[key _]]
                           (not (updated-entities key)))
                         updates))
           context
           queue-update)]
      (update resulting-state
              :internal
              (fn [current] (merge-maps current updated-entities))))))
