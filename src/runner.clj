(ns runner)

(defn build-runner [builder]
  {:updates (atom {})
   :state (atom {})
   :builder builder})

(defn- build-queue-update [runner]
  {:path []
   :function (fn [path key function]
               (swap! (:updates runner)
                      (fn [current]
                        (update-in current [path key] #(conj (or % []) function)))))})

(defn flush-runner! [runner]
  (let [updates @(:updates runner)]
    (reset! (:updates runner) {})
    (swap! (:state runner)
           (fn [state] ((:builder runner) state
                                          updates
                                          {}
                                          (build-queue-update runner))))))

(defn deref-runner! [runner limit]
  (loop [tries 0]
    (if (or (and (:value @(:state runner)) (= 0 (count @(:updates runner))))
            (> tries limit))
      (:value @(:state runner))
      (do (flush-runner! runner)
          (recur (inc tries))))))
