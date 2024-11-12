(ns scratch)

(defn q-internal [initial builder]
  ^{::type ::internal}
  (fn [state updates context queue-update]
    (let [internal (merge-maps initial (:internal state) (or (get [] updates) {}))
          wrapped-internal (map-vals internal
                                     (partial wrap-internal queue-update))]
      ((builder internal) (assoc state :internal internal)
                          updates
                          context
                          queue-update))))

(defn merge-maps [&maps]
  (reduce (fn [left right]
            (reduce (fn [acc [key value]]
                      (assoc acc key value))
                    left
                    right))
          maps))

(defn wrap-internal [queue-update value]
  ...)

(defn q-contextual [values entity]
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

(defn filter-updates [updates key]
  (->> updates (filter (fn [[item-key _]] (= (first item-key) key))) (into {})))

(defn wrap-queue-update [queue-update key]
  ...)

(defn q-entity [builder]
  ;; Consider tracking arguments in the ephemeral state and asserting that they are nil
  ;; here; then we know the entity wrapper is only called once for each time it's
  ;; overridden by a separate nested call.  Having wrapped entities should generally be
  ;; illegal, since much of the setup of the state map is designed around an individual
  ;; entity

  (fn [&arguments]
    ;; Pruning could be done at this point if needed
    ^{::type ::entity}
    (fn [state updates context queue-update]
      ((apply builder arguments) (assoc state :arguments arguments)
                                 updates
                                 context
                                 queue-update))))

(defn q-literal [value]
  ^{::type ::literal}
  (fn [state updates context queue-update]
    (assoc state :value value)))
