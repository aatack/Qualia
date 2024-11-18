(ns macros
  (:require
   [builders.contextual :refer [q-consume q-provide]]
   [builders.entity :refer [q-entity]]
   [builders.internal :refer [q-internal]]
   [builders.literal :refer [q-literal]]
   [builders.nested :refer [q-nested]]))

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

(defmacro def-context [bindings body]
  (list q-provide
        (->> bindings
             (partition 2)
             (map (fn [[key value]] [(keyword key) value]))
             (into {}))
        body))

(defmacro let-nested [bindings & body]
  (let [nested (gensym "nested")

        inbound-bindings
        (->> bindings
             (partition 2)
             (map (fn [[key value]] [(keyword key) value]))
             (into {}))

        outbound-bindings
        (->> inbound-bindings
             (mapcat (fn [[key _]] [(symbol key) `(~key ~nested)]))
             (into []))]

    `(~q-nested ~inbound-bindings
                (fn [~nested]
                  (let ~outbound-bindings ~@body)))))

(defn map-nested [function items]
  (q-nested (->> items (map function) (into {}))
            (comp q-literal identity)))
