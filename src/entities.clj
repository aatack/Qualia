(ns entities)

(defn watch-entity [key reference old-state new-state]
  (println reference "changed"))

(defn build-entity [function]
  (let [entity (atom {:function function
                      :arguments [() ()]
                      :internal {}
                      :external {}
                      :valid false
                      :value nil
                      :dependents #{}})]
    (add-watch entity :watch watch-entity)
    entity))
