(ns state.core
  (:require
   [state.entity :refer [*entity-context* build-entity-context reset-arguments!]]
   [state.watch :refer [watch-entity]]))

(defmacro defentity [name arguments & body]
  `(let [function# (fn ~arguments ~@body)]
     (def ~name (with-meta (fn [& arguments#]
                             (apply state.watch/watch-entity function# arguments#))
                  {:entity true}))))

(defn- use-cached-state [key default]
  (let [context (or *entity-context* (build-entity-context nil nil))]
    (when (not (contains? (:cache @context) key))
      (swap! context update :cache assoc key (watch-entity identity default)))
    ((:cache @context) key)))

(defmacro let-state [bindings & body]
  (let [mapped-bindings
        (->> bindings
             (partition 2)
             (mapcat (fn [[key default]]
                       [key (list state.core/use-cached-state (keyword key) default)]))
             (into []))]
    `(let ~mapped-bindings ~@body)))

(defn- use-cached-entity [key function & arguments]
  (let [context (or *entity-context* (build-entity-context nil nil))]
    (when (not (contains? (:cache @context) key))
      (swap! context update :cache assoc key
             ;; If the function is not an entity already, it needs to be wrapped
             (if (:entity (meta function))
               (apply function arguments)
               (apply watch-entity function arguments))))
    (let [entity ((:cache @context) key)]
      (apply reset-arguments! entity arguments))))

(defmacro let-entity [bindings & body]
  (let [mapped-bindings
        (->> bindings
             (partition 2)
             (mapcat (fn [[key expression]]
                       [key (list apply
                                  use-cached-entity
                                  (keyword key)
                                  (into [] expression))]))
             (into []))]
    `(let ~mapped-bindings ~@body)))

(defentity on-change [value function]
  (let-state [previous-value nil]
    (when (not= value (with-redefs [*entity-context* nil] @previous-value))
      (previous-value value)
      (with-redefs [*entity-context* nil] (function)))))

(defmacro when-changed [key test & body]
  `(let-entity [~(symbol key) (on-change ~test (fn [] ~@body))] (deref ~(symbol key))))
