(ns entity)

(defn q-entity [builder]
  ;; Consider tracking arguments in the ephemeral state and asserting that they are nil
  ;; here; then we know the entity wrapper is only called once for each time it's
  ;; overridden by a separate nested call.  Having wrapped entities should generally be
  ;; illegal, since much of the setup of the state map is designed around an individual
  ;; entity

  (fn [& arguments]
    ;; Pruning could be done at this point if needed
    ^{::type ::entity}
    (fn [state updates context queue-update]
      ((apply builder arguments) (assoc state :arguments arguments)
                                 updates
                                 context
                                 queue-update))))

(comment
  (require '[literal :refer [q-literal]]
           '[internal :refer [q-internal]])

  (assert ;; Basic entity rendering works as intended
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2"}
      (let [example
            (q-entity
             (fn [x y]
               (q-internal
                {:x x :y y}
                (fn [values] (q-literal (str @(:x values) ", " @(:y values)))))))]
        ((example 1 2) {} {} {} (fn []))))))
