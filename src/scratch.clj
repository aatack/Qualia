(ns scratch
  (:require
   [builders.internal :refer [q-internal]]
   [builders.literal :refer [q-literal]]))

(comment

  (assert
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [values] (q-literal (:x values))))
       {} {} {} (fn [])))))
