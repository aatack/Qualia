(ns helpers)

(defn merge-maps [& maps]
  (reduce (fn [left right]
            (reduce (fn [acc [key value]]
                      (assoc acc key value))
                    left
                    right))
          maps))

(defn map-vals [function mapping]
  (->> mapping
       (map (fn [[key value]] [key (function value)]))
       (into {})))

(defn map-keys [function mapping]
  (->> mapping
       (map (fn [[key value]] [(function key) value]))
       (into {})))

(defn strip-nils [mapping]
  (into {} (filter (fn [[_ value]] value) mapping)))

(defn log [value]
  (println value)
  value)

(def void-update
  {:path []
   :function (fn [path key function]
               (log {:path path :key key :function function}))})
