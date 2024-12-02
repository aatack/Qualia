(ns entities)

(defn watch-entity! [_ entity old-state new-state]
  ;; When the validity transitions from true to false, invalidate all dependents of this
  ;; entity
  (when (and (:valid old-state) (not (:valid new-state)))
    (doseq [dependent (:dependents new-state)]
      (swap! dependent assoc :valid false)))

  (let [old-dependencies (-> old-state :dependencies keys set)
        new-dependencies (-> old-state :dependencies keys set)

        removed-dependencies (reduce disj old-dependencies new-dependencies)
        added-dependencies (reduce disj new-dependencies old-dependencies)]

    ;; If any dependencies are no longer dependencies, remove this entity from their
    ;; list of dependents
    (doseq [dependency removed-dependencies]
      (swap! dependency update :dependents disj entity))

    ;; If any new dependencies have appeared, add this entity to their list of
    ;; dependents
    (doseq [dependency added-dependencies]
      (swap! dependency update :dependents conj entity))))

(defn build-entity [function & arguments]
  (let [entity (atom {:function function
                      :arguments [() arguments]
                      :entities {}
                      :valid false
                      :value nil
                      :dependencies {}
                      :dependents #{}
                      :renders 0})]
    (add-watch entity :watch watch-entity!)
    entity))

(defn build-state-entity [value]
  (build-entity (fn [arguments entities]
                  [(first arguments) entities])
                value))

(defn- recompute-entity! [entity]
  (let [[value entities]
        ((:function @entity) (-> @entity :arguments second) (:entities @entity))]
    (swap! entity (fn [current]
                    (-> current
                        (update :arguments (fn [[_ current]] [current current]))
                        (assoc :entities entities)
                        (assoc :valid true)
                        (assoc :value value)
                        (assoc :dependencies {})
                        (update :renders inc))))))

(defn evaluate-entity! [entity]
  (let [entity-state @entity
        value
        (if (:valid entity-state)
          (:value entity-state)
          (do
            (if (or (apply not= (:arguments entity-state))
                    (some (fn [[_ [reference value]]]
                            (not= (evaluate-entity! reference) value))
                          (:depdendencies entity-state)))
              (recompute-entity! entity)
              (swap! entity assoc :valid true))
            (:value @entity)))]

    ;; Save the value
    value))

(defn swap-entity! [entity function]
  (swap! entity
         (fn [current]
           (-> current
               (assoc :valid false)
               (update :arguments
                       (fn [[old-arguments new-arguments]]
                         [old-arguments (list (apply function new-arguments))]))))))

(comment

  (def e (build-state-entity 1))

  e

  (evaluate-entity! e)

  (swap-entity! e dec)

  (swap! e assoc :arguments ['(1) '(2)]))
