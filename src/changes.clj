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

(defn relevant-changes
  "Find the changes that are common to two sets."
  [listened realised]
  (cond
    (or (nil? listened) (nil? realised)) nil
    (= listened {}) realised
    (= realised {}) listened
    :else (let [key-changes-pairs
                (for [[key sub-listened] listened
                      :let [sub-realised (get realised key)
                            sub-relevant (relevant-changes sub-listened sub-realised)]
                      :when sub-relevant]
                  [key sub-relevant])]
            (if (not-empty key-changes-pairs)
              (into {} key-changes-pairs)
              nil))))
