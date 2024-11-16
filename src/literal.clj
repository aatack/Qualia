(ns literal)

(defn q-literal [value]
  ^{::type ::literal}
  (fn [state updates context queue-update]
    (assoc state :value value)))
