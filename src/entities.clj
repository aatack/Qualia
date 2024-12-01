(ns entities)

(defn watch-entity! [key reference old-state new-state]
  ;; When the validity transitions from true to false, invalidate all dependents of this
  ;; entity
  (when (and (:valid old-state) (not (:valid new-state))) 
    (doseq [dependent (:dependents new-state)]
      (swap! dependent assoc :valid false))))

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
                ;; Override `deref` here to track dependencies.  It should also store
                ;; the value of the dereferenced entity
                ((:function entity-state) (-> entity-state :arguments second)
                                          (:entities entity-state))]
            (swap! entity
                   (fn [current]
                     (-> current
                         (update :arguments (fn [[_ current]] [current current]))
                         (assoc :entities entities)
                         (assoc :valid true)
                         (assoc :value value)
                         ;; Update dependencies here
                         (update :renders inc)))))
          (:value @entity))

      :else
      (do (swap! entity assoc :valid true)
          (:value @entity)))))

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
