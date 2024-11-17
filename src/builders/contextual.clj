(ns builders.contextual
  (:require
   [helpers :refer [merge-maps]]))

(defn q-provide [values entity]
  ^{::type ::contextual}
  (fn [state updates context queue-update]
    (entity state
            updates
            (merge-maps context values)
            queue-update)))

(defn q-consume [keys builder]
  ^{::type ::consume}
  (fn [state updates context queue-update]
    (let [values (into {} (map (fn [key] [key (context key)]) keys))]
      (update ((builder values) state updates context queue-update)
              :contextual
              (fn [current] (merge-maps (or current {}) values))))))

(comment
  (require '[builders.literal :refer [q-literal]]
           '[builders.entity :refer [q-entity]])

  (assert ;; Provided values are consumed correctly, and reported
   (= {:contextual {:x 1} :value 1}
      ((q-provide
        {:x 1}
        (q-consume [:x] (fn [values] (q-literal (:x values)))))
       {} {} {} (fn []))))

  (def switch-consumption
    (q-entity (fn [on?]
                (let [key (if on? :left :right)]
                  (q-consume [key] (fn [consumed] (q-literal (key consumed))))))))

  (assert ;; Changing the consumed key correctly updates the map of dependencies
   (= {:arguments '(false), :value 2, :contextual {:right 2}, :renders 2}
      (-> {}
          ((switch-consumption true) {} {:left 1 :right 2 :other 3} (fn []))
          ((switch-consumption false) {} {:left 1 :right 2 :other 3} (fn []))))))
