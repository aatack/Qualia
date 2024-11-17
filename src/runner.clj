(ns runner)

(defn- build-queue-update [runner]
  {:path []
   :function (fn [path key function]
               (swap! (.updates runner)
                      (fn [current]
                        (update-in current [path key] #(conj (or % []) function)))))})

(defn flush-runner! [runner]
  (let [updates @(.updates runner)]
    (reset! (.updates runner) {})
    (swap! (.state runner)
           (fn [state] ((.builder runner) state
                                          updates
                                          {}
                                          (build-queue-update runner))))))

(deftype Runner [updates state builder limit]
  clojure.lang.IDeref
  (deref [this]
    (loop [tries 0]
      (if (or (and (contains? @state :value) (= 0 (count @updates)))
              (> tries limit))
        (:value @state)
        (do (flush-runner! this)
            (recur (inc tries)))))))

(defn build-runner
  ([builder limit] (Runner. (atom {}) (atom {}) builder limit))
  ([builder] (build-runner builder 10)))

(comment
  (require '[builders.literal :refer [q-literal]]
           '[builders.internal :refer [q-internal]]
           '[builders.entity :refer [q-entity]]
           '[builders.nested :refer [q-nested]])

  (def counter
    (q-entity (fn []
                (q-internal {:count 0}
                            (fn [internal]
                              (q-literal
                               {:count @(:count internal)
                                :inc (fn [] (swap! (:count internal) inc))}))))))

  (let [runner (build-runner (counter))]

    ;; The entity should initialise automatically
    (assert (= 0 (:count @runner)))

    ;; Flushing/dereferencing the runner when there are no changes should have no effect
    (assert (= 0 (:count @runner)))

    ;; Incrementing the counter should correctly update the state
    ((:inc @runner))
    (assert (= 1 (:count @runner))))

  (def nested-counter
    (q-entity (fn [] (q-nested {:a (counter)}
                               (fn [nested] (q-literal (:a nested)))))))

  (let [runner (build-runner (nested-counter))]
    ;; Calling a state update multiple times without flushing should still yield valid
    ;; results
    (let [increment (:inc @runner)]
      (increment)
      (increment)
      (increment))

    ;; Internal updates still work on nested entities
    (assert (= 3 (:count @runner))))

  (def doubly-nested-counter
    (q-entity (fn [] (q-nested {:b (nested-counter)}
                               (fn [nested] (q-literal (:b nested)))))))

  (let [runner (build-runner (doubly-nested-counter))]
    ;; Calling a state update multiple times without flushing should still yield valid
    ;; results
    (let [increment (:inc @runner)]
      (increment)
      (increment))

    ;; Internal updates still work on *doubly* nested entities
    (assert (= 2 (:count @runner))))

  (def broken-counter
    (q-entity (fn []
                (q-internal {:count 0}
                            (fn [internal]
                              (q-literal
                               {:count @(:count internal)
                                ;; The swap is not wrapped in its own function and is
                                ;; therefore called in the render function, potentially
                                ;; causing an infinite loop
                                :inc (swap! (:count internal) inc)}))))))

  (let [runner (build-runner (broken-counter))]

    ;; After cycling ten times, the limit should kick in and prevent an infinite loop
    (assert (= 10 (:count @runner)))))
