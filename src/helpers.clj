(ns helpers)

(defn merge-maps [& maps]
  (reduce (fn [left right]
            (reduce (fn [acc [key value]]
                      (assoc acc key value))
                    left
                    right))
          maps))

(defn map-vals [mapping function]
  (->> mapping
       (map (fn [[key value]] [key (function value)]))
       (into {})))

(defn log [value]
  (println value)
  value)
