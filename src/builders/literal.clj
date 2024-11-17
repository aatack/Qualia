(ns builders.literal)

(defn q-literal [value]
  ^{::type ::literal}
  (fn [state _ _ _]
    (-> state (assoc :value value) (dissoc :contextual) (dissoc :nested))))

(comment
  (require '[helpers :refer [void-update]])

  ;; Initial values are properly instantiated
  (assert (= {:value 1} ((q-literal 1) {} {} {} void-update)))

  ;; New values are correctly updated
  (assert (= {:value 2} ((q-literal 2) {:value 1} {} {} void-update))))
