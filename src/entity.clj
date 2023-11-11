(ns entity)

(defprotocol Entity
  (update [this context arguments workspace]))

(defrecord Consume [context-key workspace-key]
  Entity
  (update [_ context arguments workspace]
    [context arguments (assoc workspace workspace-key
                              (get workspace workspace-key))]))
