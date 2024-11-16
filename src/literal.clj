(ns literal)

(defn q-literal [value]
  ^{::type ::literal}
  (fn [state _ _ _]
    (assoc state :value value)))

(comment
  
  (assert (= {:value 1} ((q-literal 1) {} {} {} (fn [])))))
