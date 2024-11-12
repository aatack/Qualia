(ns scratch
  (:require [state :refer [cursor]]))

(defn q-internal [initial builder]
  (fn [state updates context queue-update]
    (let [internal (merge-maps initial ;; Override the defaults with the current state
                               (:internal state)
                               (or (get [] updates) {}))
          wrapped-internal (map-vals internal
                                     (partial wrap-internal queue-update))]
      ((builder internal) (assoc state :internal internal)
                          updates
                          context
                          queue-update))))

(defn merge-maps [&maps]
  ...)

(defn wrap-internal [queue-update value]
  ...)

(defn q-contextual [values entity]
  (fn [state updates context queue-update]
    (entity state
            updates
            (merge-maps context values)
            queue-update)))

(defn q-consume [keys builder]
  (fn [state updates context queue-update]
    (let [values (into {} (map (fn [key] [key (context key)]) keys))]
      (update ((builder values) state
                                updates
                                context
                                queue-update)
              :contextual
              (fn [contextual] (merge-maps (or contextual {}) values))))))

(defn q-nested [entities builder]
  (fn [state updates context queue-update]
    ()))

(defn q-entity [builder]
  (fn [&arguments]
    ;; Pruning could be done at this point if needed
    (fn [state updates context queue-update]
      ((apply builder arguments) (assoc state :arguments arguments)
                                 updates
                                 context
                                 queue-update))))

(defn q-literal [value]
  (fn [state updates context queue-update]
    (assoc state :value value)))

(defn example-internal [x y]
  (q-internal {:x x :y y}
              (fn [values] (str (:x values) (:y values)))))

(defn example-contextual [a]
  (q-contextual {:x 1}
                (str (example-internal 1 2) a)))

(defn example-nested [f g]
  (q-nested {:a (example-internal 1 2)
             :b (example-contextual "test")}
            (fn [values] (str (:a values) (:b values)))))
