(ns builders.literal)

(defn q-literal [value]
  ^{::type ::literal}
  (fn [state _ _ _]
    (-> state
        (assoc :value value)
        (dissoc :contextual))))

(comment

  ;; Initial values are properly instantiated
  (assert (= {:value 1} ((q-literal 1) {} {} {} (fn []))))

  ;; New values are correctly updated
  (assert (= {:value 2} ((q-literal 2) {:value 1} {} {} (fn [])))))
