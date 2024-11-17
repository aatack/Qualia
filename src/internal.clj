(ns internal
  (:require
   [helpers :refer [merge-maps]]))

(deftype InternalKeyValue [function path key value]
  clojure.lang.IDeref
  (deref [_] value))

(defn q-swap [item function]
  ((:function item) (:path item) (:key item) function))

(defn q-internal [initial builder]
  ^{::type ::internal}
  (fn [state updates context queue-update]
    (let [internal (merge-maps initial (:internal state) (or (get [] updates) {}))
          wrapped-internal (->> internal
                                (map (fn [[key value]]
                                       [key (InternalKeyValue. (:function queue-update)
                                                               (:path queue-update)
                                                               key
                                                               value)]))
                                (into {}))]
      ((builder wrapped-internal) (assoc state :internal internal)
                                  updates
                                  context
                                  queue-update))))
