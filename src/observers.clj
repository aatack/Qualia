(ns observers
  (:require [dependencies :refer [concat-dependencies conj-dependency]]))

(defprotocol Observer
  (manage [this scope changes]))

(defrecord Lookup [path]
  Observer
  (manage [_ scope _]
    {:value (get-in scope path)
     :dependencies (conj-dependency nil path)}))

(defrecord Persist [path default observer]
  Observer
  (manage [_ scope changes]
    (manage observer
            (update-in scope (apply vector :state path)
                       #(or % (:value (manage default scope changes))))
            changes)))

(defrecord Derive [function]
  Observer
  (manage [_ scope _]
    {:value (function (:workspace scope))
     :dependencies (conj-dependency nil [:workspace])}))

(defrecord Export [path property observer]
  Observer
  (manage [_ scope changes]
    (let [{:keys [value dependencies]} (manage property scope changes)]
      (manage observer
              (assoc-in scope (apply vector :state path) value)
              (concat-dependencies changes dependencies)))))
