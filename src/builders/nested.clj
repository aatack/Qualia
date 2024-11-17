(ns builders.nested
  (:require
   [helpers :refer [map-vals merge-maps]]))

(defn- wrap-queue-update [queue-update key])

(defn- filter-updates [updates key]
  (->> updates (filter (fn [[item-key _]] (= (first item-key) key))) (into {})))

(defn q-nested [entities builder]
  ^{::type ::nested}
  (fn [state updates context queue-update]
    (let [updated-entities
          (into {}
                (map (fn [[key entity]]
                       [key (entity (or (-> state :nested (get key)) {})
                                    (filter-updates updates key)
                                    context
                                    (wrap-queue-update queue-update key))])
                     entities))

          resulting-state
          ((builder (map-vals updated-entities :value))
           (update state
                   :nested
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
              :nested
              (fn [current] (merge-maps current updated-entities))))))

(comment
  (require '[builders.literal :refer [q-literal]])

  (assert
   (= {:nested {:left {:value "left"}, :right {:value "right"}}, :value "left right"}
      ((q-nested {:left (q-literal "left") :right (q-literal "right")}
                 (fn [nested] (q-literal (str (:left nested) " " (:right nested)))))
       {} {} {} (fn [])))))
