(ns changes)

(defn ->changes
  "Convert a vector path into a mapping representing a change set."
  [path]
  (if (empty? path)
    {}
    {(first path) (->changes (rest path))}))

(->changes [:a :b :c])
