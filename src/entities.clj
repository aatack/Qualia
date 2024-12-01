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

(def original-deref deref)

(defmacro capture-derefs [& body]
  `(let [derefs# (atom {})]
     (with-redefs [deref (fn [x#]
                           (swap! derefs# assoc x# (original-deref x#))
                           (original-deref x#))]
       (let [result# ~@body]
         [result# (original-deref derefs#)])
       #_(let [result# ~body]
           [result# (original-deref derefs#)]))))

(comment
  (macroexpand
   '(capture-derefs @a))

  (let [a (atom 1)
        b (atom 2)
        c (atom 3)]
    (capture-derefs (+ @a @b (first (capture-derefs (+ @b @c))))))

  (capture-derefs '(count @e))

  (def e (build-state-entity 1))

  e

  (evaluate-entity! e)

  (swap-entity! e dec)

  (swap! e assoc :arguments ['(1) '(2)]))
