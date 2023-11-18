(ns entities
  (:require [dependencies :refer [conj-dependency]]))

(defprotocol Entity
  (manage [this scope changes]))

(defrecord Consume [context-key workspace-key entity]
  Entity
  (manage [_ scope changes]
    (throw (Exception. "Not implemented"))))

(defrecord Child [key definition bindings])
