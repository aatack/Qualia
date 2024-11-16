(ns literal)

(defn q-literal [value]
  ^{::type ::literal}
  (fn [state _ _ _]
    (assoc state :value value)))

(comment

  ;; Initial value is properly instantiated
  (assert (= {:value 1} ((q-literal 1) {} {} {} (fn []))))

  ;; New value is correctly updates
  (assert (= {:value 2} ((q-literal 2) {:value 1} {} {} (fn [])))))
