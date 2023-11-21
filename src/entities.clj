(ns entities
  (:require [dependencies :refer [conj-dependency]]))

(defprotocol Entity
  (manage [this scope changes]))

(defrecord Lookup [path]
  Entity
  (manage [_ scope _]
    {:value (get-in scope path)
     :dependencies (conj-dependency nil path)}))

(defrecord Child [key definition bindings])
