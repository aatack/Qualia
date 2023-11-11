(ns entities
  (:require [dependencies :refer [conj-dependency]]))

(defprotocol Entity
  (manage [this context arguments workspace]))

(defrecord Consume [context-key workspace-key entity]
  Entity
  (manage [_ context arguments workspace]
    (manage entity context arguments
            (-> workspace
                (assoc workspace-key
                       (get context context-key))
                (conj-dependency :context context-key)))))
