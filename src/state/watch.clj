(ns state.watch
  (:import [state.entity Entity]))

(defn- watch-entity-state [_ entity old-state new-state]
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
      (swap! (:state dependency) update :dependents #(disj (or % #{}) entity)))

    ;; If any new dependencies have appeared, add this entity to their list of
    ;; dependents
    (doseq [dependency added-dependencies]
      (swap! (:state dependency) update :dependents #(conj (or % #{}) entity)))))

(defn watch-entity [function & arguments]
  (let [state (atom {:arguments [nil arguments]})]
    (add-watch state :watch watch-entity-state)
    (Entity. function state)))
