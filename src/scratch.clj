(ns scratch)

(defn example-child []
  (let [count (atom 0)]
    (fn [target]
      {:value (str "Target " target " count " @count)
       :handle (fn [key]
                 (println "-->" key)
                 (when (= key target)
                   (swap! count inc)))})))

(defn example []
  (let [letters (atom [])
        cache (atom {})
        watch (fn [key head & body]
                (when-not (contains? @cache key)
                  (swap! cache assoc key (head)))
                (apply (get @cache key) body))]
    (fn []
      (let [children (map (fn [letter]
                            (watch letter example-child letter))
                          @letters)]
        (println (type children))
        {:value (str "Count: " @letters (into [] (map :value children)))
         :handle (fn [key]
                   (println "Key" key)
                   (when (= (str (first key)) "+")
                     (swap! letters conj (str (second key))))
                   (println "Children" children)
                   (doall (for [child children]
                            (do (println "Running" child key)
                                ((:handle child) key)))))}))))

(comment

  (def app (example))

  (def c (example-child))

  (app)

  (c "f")

  (def _ ((:handle (app)) "+c"))
  (def _ ((:handle (app)) "c"))



  ((:handle (c "f")) "f"))
