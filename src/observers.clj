(ns observers
  (:require [dependencies :refer [conj-dependency]]))

(defprotocol Observer
  (manage [this scope changes]))

(defrecord Lookup [path]
  Observer
  (manage [_ scope _]
    {:value (get-in scope path)
     :dependencies (conj-dependency nil path)}))
