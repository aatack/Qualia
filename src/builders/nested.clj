(ns builders.nested
  (:require
   [helpers :refer [map-keys map-vals merge-maps strip-nils]]))

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
      (-> resulting-state
          (update :nested
                  (fn [current] (merge-maps current updated-entities)))
          (update :contextual
                  (fn [current] (apply merge-maps current
                                       (map :contextual (vals updated-entities)))))
          strip-nils))))

(comment
  (require '[builders.literal :refer [q-literal]]
           '[builders.internal :refer [q-internal]]
           '[builders.entity :refer [q-entity]]
           '[builders.contextual :refer [q-consume q-provide]])

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
       ;; The left nested entity should be rendered twice as it was updated; but the
       ;; right nested entity should only be rendered once.  The second update doesn't
       ;; affect it
       {:left {:arguments '(1) :internal {:x 2} :value 2 :renders 2}
        :right {:arguments '(5) :internal {:x 5} :value 5 :renders 1}}
       :value "2 = 5"}
      (-> {}
          (nested-entity {} {} (fn []))
          (nested-entity {'(:left) {:x [inc]}} {} (fn [])))))

  (def nested-contextual
    (let [entity (q-entity
                  (fn [key]
                    (q-consume [key] (fn [consumed] (q-literal (key consumed))))))]
      (q-provide
       {:a 5 :b 8}
       (q-nested
        {:left (entity :a) :right (entity :b)}
        (fn [nested] (q-literal (str (:left nested) " " (:right nested))))))))

  (assert ;; Nested entities correctly consume provided values
   (= {:nested
       {:left {:arguments '(:a) :value 5 :contextual {:a 5} :renders 1}
        :right {:arguments '(:b) :value 8 :contextual {:b 8} :renders 1}}
       :value "5 8"
       :contextual {}}
      (nested-contextual {} {} {} (fn []))))

  (def nested-contextual-entity
    (let [entity (q-entity
                  (fn [key]
                    (q-consume [key] (fn [consumed] (q-literal (key consumed))))))]
      (q-nested
       {:left (entity :a) :right (entity :b)}
       (fn [nested] (q-literal (str (:left nested) " " (:right nested)))))))

  (assert ;; Nested entities are pruned when their consumed keys do not change
   (= {:nested
       {:left {:arguments '(:a) :value 42 :contextual {:a 42} :renders 2}
        :right {:arguments '(:b) :value 8 :contextual {:b 8} :renders 1}}
       :value "42 8"
       :contextual {:a 42 :b 8}}
      (-> {}
          (nested-contextual-entity {} {:a 5 :b 8} (fn []))
          (nested-contextual-entity {} {:a 42 :b 8} (fn [])))))

  (def child
    (q-entity
     (fn []
       (q-internal {:count 0}
                   (fn [internal] (q-literal @(:count internal)))))))

  (def parent
    (q-entity
     (fn [keys] (q-nested (->> keys (map (fn [key] [key (child)])) (into {}))
                          q-literal))))

  (def grandparent
    (q-entity (fn [left right] (q-nested {:left (parent left)
                                          :right (parent right)}
                                         q-literal))))

  (assert ;; Nested entities that are no longer used should be removed from the state
   (= [:a :b :f]
      (-> {}
          ((grandparent [:a :b :c] [:x :y :z]) {} {} (fn []))
          ((grandparent [:a :b :f] [:x :y :z]) {} {} (fn []))
          :nested :left :nested keys sort)))

  (let [result
        (-> {}
            ((grandparent [:a :b :c] [:x :y :z]) {} {} (fn []))
            ((grandparent [:a :b :c] [:x :y :z])
             {'(:left :a) {:count [inc]}} {} (fn []))
            ((grandparent [:a :b :c] [:x :y :z])
             {'(:left :a) {:count [inc]}} {} (fn [])))]

    (assert ;; The entity whose state was updated has been rendered thrice now
     (= 3
        (-> result :nested :left :renders)
        (-> result :nested :left :nested :a :renders)))

    (assert ;; Other entities have only been rendered once
     (= 1
        (-> result :nested :right :renders)
        (-> result :nested :left :nested :b :renders)))
    
    (assert ;; The value has been updated correctly
     (= {:left {:a 2 :b 0 :c 0} :right {:x 0 :y 0 :z 0}}
        (:value result)))))
