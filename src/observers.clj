(ns observers
  (:require [changes :refer [as-changes merge-changes relevant-changes]]))

(defprotocol Observer
  (manage [this scope changes]))

(defrecord Lookup [path]
  Observer
  (manage [_ scope _]
    {:value (get-in scope path)
     :changes (as-changes path)}))

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
     :changes (as-changes [:workspace])}))

(defrecord Write [path property observer]
  Observer
  (manage [_ scope changes]
    (let [path-changes (as-changes path)
          managed-property (manage property scope changes)

          managed-observer
          (manage observer
                  (assoc-in scope (apply vector :state path) (:value managed-property))
                  (merge-changes changes
                                 (when (relevant-changes (:changes managed-property)
                                                         changes)
                                   (path-changes))))]
      {:value (:value managed-observer)
       :changes (if (relevant-changes (:changes managed-property)
                                      (:changes managed-observer))
                  (merge-changes (:changes managed-observer)
                                 (:changes managed-property))
                  (:changes managed-observer))})))
