(ns builders.literal)

(defn q-literal [value]
  ^:qualia
  (fn [state _ _ _]
    (-> state (assoc :value value) (dissoc :contextual) (dissoc :nested))))

(defn wrap-literal [value]
  (if (-> value meta :qualia) value (q-literal value)))

(comment
  (require '[helpers :refer [void-update]])

  ;; Initial values are properly instantiated
  (assert (= {:value 1} ((q-literal 1) {} {} {} void-update)))

  ;; New values are correctly updated
  (assert (= {:value 2} ((q-literal 2) {:value 1} {} {} void-update))))
