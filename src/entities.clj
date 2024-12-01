(ns entities)

(defn watch-entity! [key reference old-state new-state]
  (println reference "changed"))

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

(defn evaluate-entity! [entity]
  (let [entity-state @entity]
    (cond
      (:valid entity-state) (:value entity-state)

      (or (apply not= (:arguments entity-state))
          (some (fn [[_ [reference value]]] (not= (evaluate-entity! reference) value))
                (:internal entity-state)))
      (do (let [[value entities]
                ;; Override `deref` here to track dependencies
                ((:function entity-state) (-> entity-state :arguments second)
                                          (:entities entity-state))]
            (swap! entity
                   (fn [current]
                     (-> current
                         (assoc :value value)
                         (assoc :valid true)
                         (assoc :entities entities)
                         (update :renders inc)))))
          (:value @entity))

      :else
      (do (swap! entity assoc :valid true)
          (:value @entity)))))

(defn swap-entity! [entity function])

(comment

  (def e (build-state-entity 1))

  e

  (evaluate-entity! e)

  (swap! e assoc :arguments ['(1) '(2)]))
