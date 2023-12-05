(ns observers
  (:require [changes :refer [as-changes merge-changes relevant-changes]]))

(defprotocol Observer
  (manage [this scope changes]))

(defrecord Lookup [path]
  Observer
  (manage [_ scope _]
    {::value (get-in scope path)
     ::changes (as-changes path)}))

(defrecord Derive [function]
  Observer
  (manage [_ scope _]
    {::value (function (::workspace scope))
     ::changes (as-changes [::workspace])}))

(defrecord Persist [path default observer]
  Observer
  (manage [_ scope changes]
    (manage observer
            (update-in scope (apply vector ::state path)
                       #(or % (::value (manage default scope changes))))
            changes)))

(defrecord Write [path property lazy observer]
  Observer
  (manage [_ scope changes]
    (let [path-changes (as-changes path)]
      (if (and lazy (not (relevant-changes (-> scope ::workspace ::export)
                                           path-changes)))
        (manage observer scope changes)
        (let [managed-property (manage property scope changes)

              managed-observer
              (manage observer
                      (assoc-in scope path (::value managed-property))
                      (merge-changes
                       changes
                       (when
                        (relevant-changes (::changes managed-property) changes)
                         path-changes)))]
          {::value (::value managed-observer)
           ::changes (if (relevant-changes path-changes (::changes managed-observer))
                       (merge-changes (::changes managed-observer)
                                      (::changes managed-property))
                       (::changes managed-observer))})))))

(defrecord Cache [observer]
  Observer
  (manage [_ scope changes]
    (let [rerun (or (nil? (::state scope))
                    (relevant-changes (-> scope ::state ::changes) changes))
          managed-observer (manage observer scope changes)]
      (if rerun
        {::value (assoc-in (::value managed-observer)
                           [::state ::changes] (::changes managed-observer))
         ::changes (::changes managed-observer)}
        (-> scope ::state ::value)))))

(defrecord Call [path inputs child observer]
  Observer
  (manage [_ scope changes]
    (let [path-changes (as-changes path)]
      (manage observer scope changes))))
