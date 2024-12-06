(ns entities)

(def ^:dynamic *entity-context* nil)

(defn- dependencies-changed?
  "Determine whether the dependencies or arguments in the state have actually changed.
   
   Sometimes, one of the dependencies will become invalid, but its value will not
   actually have changed.  Therefore it's not actually necessary to recompute the value
   of dependent entities."
  [state]
  ;; When evaluating the dependencies just to check if they've changed, there's no need
  ;; to track dereferenced entities
  (with-redefs [*entity-context* nil]
    (or (apply not= (:arguments state))
        (some (fn [[entity value]] (not= @entity value))
              (:dependencies state)))))

(defn- recompute-entity!
  "Recompute the value in the entity, updating its state, and return the result."
  [entity]
  (let [context (atom {:dependencies {} :cache {}})]
    (with-redefs [*entity-context* context]
      (let [arguments (-> @(:state entity) :arguments second)
            value (apply (:function entity) arguments)]

        (swap! (:state entity)
               (fn [current]
                 (-> current
                     (assoc :value value)
                     (assoc :valid true)
                     (assoc :arguments [arguments arguments])
                     (assoc :dependencies (:dependencies @context))
                     (assoc :cache (:cache @context))
                     (update :renders #(inc (or % 0))))))

        value))))

(defn reset-arguments! [entity & new-arguments]
  (swap! (:state entity)
         (fn [current]
           (-> current
               (assoc :valid false)
               (update :arguments
                       (fn [[old-arguments _]]
                         [old-arguments new-arguments]))))))

(defrecord Entity [function state]
  clojure.lang.IDeref
  (deref [this]
    (let [value (cond (:valid @state) (:value @state)
                      (dependencies-changed? @state) (recompute-entity! this)
                      :else (do (swap! state assoc :valid true) (:value @state)))]

      ;; If there's currently a context in effect, this value is being referenced by an
      ;; entity, and it needs to be added to the list of dependencies (along with the
      ;; value it took at the time of the evaluation)
      (when *entity-context*
        (swap! *entity-context* update :dependencies assoc this value))

      value)))

(defn- watch-state [_ entity old-state new-state]
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

(defn ->entity [function & arguments]
  (let [state (atom {:arguments [nil arguments]})]
    (add-watch state :watch watch-state)
    (Entity. function state)))

(defn ->state [value]
  (->entity identity value))

(defn printentity [entity]
  (-> @(:state entity)
      (update :dependents count)
      (update :dependencies count)))

(comment
  (def a (->state 1))
  (def b (->state 2))
  (def c (->state 3))

  (reset-arguments! e a c c)
  (reset-arguments! a 5)

  (def e (->entity (fn [x y z] (+ @x @y @z)) a b c))

  (printentity a)

  @e
  (:renders @(:state e))

  (->> @(:state e) :cache keys))
