(ns builders.internal
  (:require
   [helpers :refer [merge-maps]])
  (:import [clojure.lang IAtom]))

(deftype InternalKeyValue [function path key value]
  clojure.lang.IDeref
  (deref [_] value)

  IAtom
  (reset [_ v]
    (function path key (constantly v)))
  (swap [_ f]
    (function path key f))
  (swap [_ f x]
    (function path key (partial f x)))
  (swap [_ f x y]
    (function path key (partial f x y))))

(defn q-internal [initial builder]
  ^:qualia
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
  #_{:clj-kondo/ignore [:duplicate-require]}
  (require '[builders.literal :refer [q-literal]]
           '[helpers :refer [void-update]])

  (assert ;; New values are initialised properly
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [_] (q-literal 1)))
       {} {} {} void-update)))

  (assert ;; Already-existing values are not overridden by the initial values
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 2} (fn [_] (q-literal 1)))
       {:internal {:x 1}} {} {} void-update)))

  (assert ;; Dereferencing internal state works
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [internal] (q-literal @(:x internal))))
       {} {} {} void-update)))

  (assert ;; Updates are applied in the correct order, and make it through to the values
   (= {:value 4 :internal {:x 4}}
      ((q-internal {:x 2} (fn [internal] (q-literal @(:x internal))))
       {:internal {:x 1}} {() {:x [inc (partial * 2)]}} {} void-update))))
