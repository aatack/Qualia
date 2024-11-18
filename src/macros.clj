(ns macros
  (:require
   [builders.contextual :refer [q-consume q-provide]]
   [builders.entity :refer [q-entity]]
   [builders.internal :refer [q-internal]]
   [builders.literal :refer [q-literal wrap-literal]]
   [builders.nested :refer [q-nested]]))

(defmacro defentity [name args & body]
  `(def ~name (~q-entity (fn ~args (~wrap-literal (do ~@body))))))

(defmacro let-internal [bindings & body]
  (let [internals (gensym "internal")
        inbound-bindings (->> bindings
                              (partition 2)
                              (map (fn [[key value]] [(keyword key) value]))
                              (into {}))
        outbound-bindings (->> inbound-bindings
                               (mapcat (fn [[key _]] [(symbol key) `(~key ~internals)]))
                               (into []))]
    `(~q-internal ~inbound-bindings
                  (fn [~internals]
                    (let ~outbound-bindings (~wrap-literal (do ~@body)))))))

(defmacro let-context [keys & body]
  (let [consumed (gensym "consumed")
        keywords (into [] (map keyword keys))
        symbols (->> keywords
                     (mapcat (fn [key] [(symbol key) `(~key ~consumed)]))
                     (into []))]
    `(~q-consume ~keywords
                 (fn [~consumed] (let ~symbols (~wrap-literal (do ~@body)))))))

(defmacro def-context [bindings body]
  (list q-provide
        (->> bindings
             (partition 2)
             (map (fn [[key value]] [(keyword key) value]))
             (into {}))
        (list wrap-literal body)))

(defmacro let-nested [bindings & body]
  (let [nested (gensym "nested")
        keywords (->> bindings
                      (partition 2)
                      (map (fn [[key value]] [(keyword key) value]))
                      (into {}))
        symbols (->> keywords
                     (mapcat (fn [[key _]] [(symbol key) `(~key ~nested)]))
                     (into []))]
    `(~q-nested ~keywords
                (fn [~nested]
                  (let ~symbols (~wrap-literal (do ~@body)))))))

(defn map-nested [function items]
  (q-nested (->> items (map function) (into {}))
            (comp q-literal identity)))
