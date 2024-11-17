(ns builders.entity)

(defn q-entity [builder]
  (fn [& arguments]
    ^{::type ::entity}
    (fn [state updates context queue-update]
      (let [arguments-changed? (not= (or arguments ()) (:arguments state))
            has-updates? (> (count updates) 0)
            context-changed? (->> (or (:contextual state) {})
                                  (some (fn [[key value]] (not= (context key) value))))]
        (if (or arguments-changed? has-updates? context-changed?)
          (-> ((apply builder arguments) (assoc state :arguments (or arguments ()))
                                         updates
                                         context
                                         queue-update)
              (update :renders #(inc (or % 0))))
          ;; If the arguments haven't changed, there are no updates targeting this sub-
          ;; tree, and none of the consumed contextual values have changed, there is no
          ;; need to re-render this entity
          state)))))

(comment
  (require '[builders.literal :refer [q-literal]]
           '[builders.internal :refer [q-internal]]
           '[builders.contextual :refer [q-consume]])

  (def example
    (q-entity
     (fn [x y]
       (q-internal
        {:x x :y y}
        (fn [internal]
          (q-consume
           [:a]
           (fn [consumed]
             (q-literal
              (str @(:x internal)
                   ", " @(:y internal)
                   ", " (or (:a consumed) "_"))))))))))

  (assert ;; Basic entity rendering works as intended
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2, _"
       :contextual {:a nil}
       :renders 1}
      ((example 1 2) {} {} {} (fn []))))

  (assert ;; Rendering multiple times with no updates does not change the state
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2, _"
       :contextual {:a nil}
       :renders 1} ;; Should only render once since the second render has no changes
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 1 2) {} {} (fn [])))))

  (assert ;; Rendering with updates correctly propagates changes to the internal state
   (= {:arguments '(1 2)
       :internal {:x 2 :y 1}
       :value "2, 1, _"
       :contextual {:a nil}
       :renders 2}
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 1 2) {() {:x [inc] :y [dec]}} {} (fn [])))))

  (assert ;; Rendering with context changes correctly updates the state
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2, 5"
       :contextual {:a 5}
       :renders 2}
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 1 2) {} {:a 5} (fn [])))))

  (assert ;; Setting a contextual value to the same value it was before does not render
   (= {:arguments '(1 2)
       :internal {:x 1 :y 2}
       :value "1, 2, 5"
       :contextual {:a 5}
       :renders 2}
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 1 2) {} {:a 5} (fn []))
          ((example 1 2) {} {:a 5} (fn [])))))

  (assert ;; Rendering with changes to the arguments should update the state
   (= {:arguments '(2 2)
       :internal {:x 1 :y 2}
       :value "1, 2, _"
       :contextual {:a nil}
       :renders 2} ;; Should only render once since the second render has no changes
      (-> {}
          ((example 1 2) {} {} (fn []))
          ((example 2 2) {} {} (fn [])))))

  (assert ;; Zero-argument entities render correctly
   (= {:arguments () :value 8 :renders 1}
      (((q-entity (fn [] (q-literal 8)))) {} {} {} (fn [])))))