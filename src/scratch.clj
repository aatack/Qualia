(ns scratch 
  (:require
   [internal :refer [q-internal]]
   [literal :refer [q-literal]]))

(comment

  (assert (= {:value 1} ((q-literal 1) {} {} {} (fn []))))

  (assert
   (= {:value 1 :internal {:x 1}}
      ((q-internal {:x 1} (fn [values] (q-literal (:x values))))
       {} {} {} (fn [])))))
