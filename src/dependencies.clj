(ns dependencies
  (:require [clojure.set :refer [union]]))

(def empty-dependencies #{})

(defn conj-dependency [dependencies path]
  (let [wrapped-path (if (vector? path) path [path])]
    (conj (or dependencies empty-dependencies) wrapped-path)))

(defn concat-dependencies [left right]
  (union left right))
