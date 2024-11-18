(ns macros
  (:require
   [builders.entity :refer [q-entity]]
   [builders.internal :refer [q-internal]]
   [builders.literal :refer [q-literal]]
   [helpers :refer [void-update]]))

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
