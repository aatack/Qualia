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
    (let [internal (reduce (fn [values [key functions]]
                             (update values key (apply comp (reverse functions))))
                           (merge-maps initial (:internal state))
                           (or (get [] updates) {}))
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

(comment
  (require '[literal :refer [q-literal]])

  (assert ;; Check that new values are initialised properly
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [values] (q-literal @(:x values))))
       {} {} {} (fn [])))))
