(ns changes)

(defn ->changes
  "Convert a vector path into a mapping representing a change set."
  [path]
  (cond
    (nil? path) nil
    (empty? path) {}
    :else {(first path) (->changes (rest path))}))

(defn merge-changes
  "Merge two sets of changes together."
  [left right]
  (cond
    (or (= left {}) (= right {})) {}
    (nil? left) right
    (nil? right) left
    :else (let [all-keys (concat (keys left) (keys right))]
            (into {}
                  (map (fn [key] [key (merge-changes (get left key) (get right key))])
                       all-keys)))))
