(ns builders.contextual
  (:require
   [helpers :refer [merge-maps]]))

(defn q-provide [values entity]
  ^{::type ::contextual}
  (fn [state updates context queue-update]
    (-> (entity state
                updates
                (merge-maps context values)
                queue-update)
        ;; No longer depend on any keys that are provided by this builder
        (update :contextual (fn [current]
                              (reduce dissoc current (keys values)))))))

(defn q-consume [keys builder]
  ^{::type ::consume}
  (fn [state updates context queue-update]
    (let [values (into {} (map (fn [key] [key (context key)]) keys))]
      (update ((builder values) state updates context queue-update)
              :contextual
              (fn [current] (merge-maps (or current {}) values))))))

(comment
  #_{:clj-kondo/ignore [:duplicate-require]}
  (require '[builders.literal :refer [q-literal]]
           '[builders.entity :refer [q-entity]]
           '[helpers :refer [void-update]])

  (assert ;; Provided values are consumed correctly, and reported
   (= {:contextual {} :value 1}
      ((q-provide
        {:x 1}
        (q-consume [:x] (fn [values] (q-literal (:x values)))))
       {} {} {} void-update)))

  (def switch-consumption
    (q-entity (fn [on?]
                (let [key (if on? :left :right)]
                  (q-consume [key] (fn [consumed] (q-literal (key consumed))))))))

  (assert ;; Changing the consumed key correctly updates the map of dependencies
   (= {:arguments '(false), :value 2, :contextual {:right 2}, :renders 2}
      (-> {}
          ((switch-consumption true) {} {:left 1 :right 2 :other 3} void-update)
          ((switch-consumption false) {} {:left 1 :right 2 :other 3} void-update))))

  (def nested-contexts
    (q-provide {:a 1}
               (q-consume [:a :b] (fn [context] (q-literal (:a context))))))

  (assert ;; Entities do not report dependencies on keys they provide
   (= {:value 1 :contextual {:b 5}}
      (nested-contexts {} {} {:b 5} void-update))))
