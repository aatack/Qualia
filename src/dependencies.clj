(ns dependencies)

(defn conj-dependency [workspace kind path]
  (let [wrapped-path (if (vector? path) path [path])]
    (update-in workspace [::dependencies kind]
               #(conj (or % #{}) wrapped-path))))
