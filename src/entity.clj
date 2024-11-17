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

  (def example
    (q-entity
     (fn [x y]
       (q-internal
        {:x x :y y}
        (fn [values] (q-literal (str @(:x values) ", " @(:y values))))))))

  (assert ;; Basic entity rendering works as intended
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2"}
      ((example 1 2) {} {} {} (fn []))))

  (assert ;; Rendering multiple times with no updates does not change the state
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2"}
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 1 2) {} {} (fn [])))))

  (assert ;; Rendering with updates correctly propagates changes to the internal state
   (= {:arguments '(1 2)
       :internal {:x 2 :y 1}
       :value "2, 1"}
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 1 2) {() {:x [inc] :y [dec]}} {} (fn []))))))
