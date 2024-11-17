(ns builders.nested
  (:require
   [helpers :refer [map-keys map-vals merge-maps]]))

(defn- wrap-queue-update [queue-update key])

(defn- filter-updates [updates key]
  (->> updates
       (filter (fn [[item-key _]] (= (first item-key) key)))
       (map-keys rest)
       (into {})))

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
          ((builder (map-vals :value updated-entities))
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
  (require '[builders.literal :refer [q-literal]]
           '[builders.internal :refer [q-internal]]
           '[builders.entity :refer [q-entity]])

  (assert ;; The most basic possible nested values return correctly
   (= {:nested {:left {:value "left"} :right {:value "right"}} :value "left right"}
      ((q-nested {:left (q-literal "left") :right (q-literal "right")}
                 (fn [nested] (q-literal (str (:left nested) " " (:right nested)))))
       {} {} {} (fn []))))

  (def nested-internal
    (q-nested {:left (q-internal {:x 1} (fn [internal]
                                          (q-literal @(:x internal))))
               :right (q-internal {:x 5} (fn [internal]
                                           (q-literal (str " = " @(:x internal)))))}
              (fn [nested] (q-literal (str (:left nested) (:right nested))))))

  (assert ;; Nested internal state is correctly updated
   (= {:nested {:left {:internal {:x 2} :value 2}
                :right {:internal {:x 5} :value " = 5"}}
       :value "2 = 5"}
      (-> {}
          (nested-internal {} {} (fn []))
          (nested-internal {'(:left) {:x [inc]}} {} (fn [])))))

  (def nested-entity
    (let [entity (q-entity
                  (fn [x] (q-internal
                           {:x x}
                           (fn [internal]
                             (q-literal @(:x internal))))))]
      (q-nested {:left (entity 1)
                 :right (entity 5)}
                (fn [nested] (q-literal (str (:left nested) " = " (:right nested)))))))

  (assert ;; Prunes work correctly on internal updates within nested entities
   (= {:nested
       ;; The left nested entity should be rendered twice, as it was updated; but the
       ;; right nested entity should only be rendered once.  The second update doesn't
       ;; affect it
       {:left {:arguments '(1) :internal {:x 2} :value 2 :renders 2}
        :right {:arguments '(5) :internal {:x 5} :value 5 :renders 1}}
       :value "2 = 5"}
      (-> {}
          (nested-entity {} {} (fn []))
          (nested-entity {'(:left) {:x [inc]}} {} (fn []))))))
