(ns macros
  (:require
   [builders.contextual :refer [q-consume]]
   [builders.entity :refer [q-entity]]
   [builders.internal :refer [q-internal]]))

(defmacro defentity [name args & body]
  `(def ~name (~q-entity (fn ~args ~@body))))

(defmacro let-internal [bindings & body]
  (let [internals (gensym "internal")

        inbound-bindings
        (->> bindings
             (partition 2)
             (map (fn [[key value]] [(keyword key) value]))
             (into {}))

        outbound-bindings
        (->> inbound-bindings
             (mapcat (fn [[key _]] [(symbol key) `(~key ~internals)]))
             (into []))]

    `(~q-internal ~inbound-bindings
                  (fn [~internals]
                    (let ~outbound-bindings ~@body)))))

(defmacro let-context [keys & body]
  (let [consumed (gensym "consumed")
        inbound-bindings (into [] (map keyword keys))
        outbound-bindings (->> inbound-bindings
                               (mapcat (fn [key] [(symbol key) `(~key ~consumed)]))
                               (into []))]
    `(~q-consume ~inbound-bindings (fn [~consumed] (let ~outbound-bindings ~@body)))))
