(ns state.entity)

(def ^:dynamic *entity-context* nil)

(defn- build-entity-context [dependencies cache]
  (atom {:dependencies (or dependencies {}) :cache (or cache {})}))

(defn- dependencies-changed?
  "Determine whether the dependencies or arguments in the state have actually changed.
   
   Sometimes, one of the dependencies will become invalid, but its value will not
   actually have changed.  Therefore it's not actually necessary to recompute the value
   of dependent entities."
  [state]
  ;; When evaluating the dependencies just to check if they've changed, there's no need
  ;; to track dereferenced entities
  (with-redefs [*entity-context* nil]
    (or (not (contains? state :value))
        (apply not= (:arguments state))
        (some (fn [[entity value]] (not= @entity value))
              (:dependencies state)))))

(defn- recompute-entity!
  "Recompute the value in the entity, updating its state, and return the result."
  [entity]
  (let [state @(:state entity)
        context (build-entity-context (:dependencies state) (:cache state))]
    (with-redefs [*entity-context* context]
      (let [arguments (-> state :arguments second)
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
  (when (not= new-arguments (-> entity :state deref :arguments second))
    (swap! (:state entity)
           (fn [current]
             (-> current
                 (assoc :valid false)
                 (update :arguments
                         (fn [[old-arguments _]]
                           [old-arguments new-arguments]))))))
  entity)

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

      value))

  clojure.lang.IFn
  (invoke [& arguments]
    (reset-arguments! (Entity. function state) arguments)))

