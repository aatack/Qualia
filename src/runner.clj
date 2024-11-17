(ns runner)

(defn build-runner [builder]
  {:updates (atom {})
   :state (atom {})
   :builder builder})

(defn- build-queue-update [runner]
  (fn [path key function]
    (swap! (:updates runner)
           (fn [current]
             (update-in current [path key] #(conj (or % []) function))))))

(defn flush-runner! [runner]
  (let [updates @(:updates runner)
        queue-update {:path []
                      :function (fn [path key function]
                                  (swap! (:updates runner)
                                         (fn [current])))}]
    (reset! (:updates runner) {})
    (swap! (:state runner)
           (fn [state] ((:builder runner) state updates {} queue-update)))))
