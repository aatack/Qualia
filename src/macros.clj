(ns macros
  (:require
   [builders.entity :refer [q-entity]]
   [builders.literal :refer [q-literal]]
   [runner :refer [build-runner]]))

(defmacro defentity [name args & body]
  `(def ~name (~q-entity (fn ~args ~@body))))
