(ns entities)

(defn watch-entity! [_ entity old-state new-state]
  ;; When the validity transitions from true to false, invalidate all dependents of this
  ;; entity
  (when (and (:valid old-state) (not (:valid new-state)))
    (doseq [dependent (:dependents new-state)]
      (swap! dependent assoc :valid false)))

  (let [old-dependencies (-> old-state :dependencies keys set)
        new-dependencies (-> new-state :dependencies keys set)

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

(def tracked-dependencies (atom {}))

(defn- recompute-entity! [entity]
  (let [local-tracked-dependencies (atom {})
        [value entities]
        (with-redefs [tracked-dependencies local-tracked-dependencies]
          ((:function @entity) (-> @entity :arguments second) (:entities @entity)))]
    (swap! entity (fn [current]
                    (-> current
                        (update :arguments (fn [[_ current]] [current current]))
                        (assoc :entities entities)
                        (assoc :valid true)
                        (assoc :value value)
                        (assoc :dependencies @local-tracked-dependencies)
                        (update :renders inc))))))

(defn evaluate-entity! [entity track-dependencies?]
  (let [entity-state @entity
        value
        (if (:valid entity-state)
          (:value entity-state)
          (do
            (if (or (apply not= (:arguments entity-state))
                    (some (fn [[reference value]]
                            (not= (evaluate-entity! reference false) value))
                          (:dependencies entity-state)))
              (recompute-entity! entity)
              (swap! entity assoc :valid true))
            (:value @entity)))]

    (when track-dependencies?
      (swap! tracked-dependencies assoc entity value))
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

  (def a (build-state-entity 1))
  (def b (build-state-entity 2))

  (do (swap-entity! a inc) nil)

  (def e (build-entity (fn [arguments entities]
                         [(> 100 (apply +
                                 (evaluate-entity! a true)
                                 (evaluate-entity! b true)
                                 arguments))
                          entities])))
  (def f (build-entity (fn [arguments entities]
                         [(if (evaluate-entity! e true) :yes :no)
                          entities])))

  (-> @a (update :dependents count) (update :dependencies count))
  (-> @f (update :dependents count) (update :dependencies count))

  (evaluate-entity! f false)

  (do (swap! e (fn [ee] (-> ee
                            (assoc :arguments [nil '(5 6)])
                            (assoc :valid false)))) nil))
