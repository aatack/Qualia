(ns builders.internal
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
                           (or (get updates ()) {}))
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
  (require '[builders.literal :refer [q-literal]])

  (assert ;; New values are initialised properly
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [_] (q-literal 1)))
       {} {} {} (fn []))))

  (assert ;; Already-existing values are not overridden by the initial values
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 2} (fn [_] (q-literal 1)))
       {:internal {:x 1}} {} {} (fn []))))

  (assert ;; Dereferencing internal state works
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [internal] (q-literal @(:x internal))))
       {} {} {} (fn []))))

  (assert ;; Updates are applied in the correct order, and make it through to the values
   (= {:value 4 :internal {:x 4}}
      ((q-internal {:x 2} (fn [internal] (q-literal @(:x internal))))
       {:internal {:x 1}} {() {:x [inc (partial * 2)]}} {} (fn [])))))
