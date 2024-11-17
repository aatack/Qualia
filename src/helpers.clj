(ns helpers)

(defn merge-maps [& maps]
  (reduce (fn [left right]
            (reduce (fn [acc [key value]]
                      (assoc acc key value))
                    left
                    right))
          maps))
