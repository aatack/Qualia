(ns entities 
  (:require [dependencies :refer [conj-dependency]]))

(defprotocol Entity
  (update [this context arguments workspace]))

(defrecord Consume [context-key workspace-key]
  Entity
  (update [_ context arguments workspace]
    [context arguments
     (-> workspace
         (assoc workspace-key
                              (get context context-key))
         (conj-dependency :context context-key))]))
