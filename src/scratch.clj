(ns scratch)

(defn watcher []
  (let [cache (atom {})]
    (fn [key head & body]
      (when-not (contains? @cache key)
        (swap! cache assoc key (head)))
      (apply (get @cache key) body))))

(defn example-child []
  (let [count (atom 0)]
    (fn [target]
      {:value (str "Target " target " count " @count)
       :handle (fn [key]
                 (when (= key target)
                   (swap! count inc)))})))

(defn example []
  (let [letters (atom [])
        watch (watcher)]
    (fn []
      (let [children (map (fn [letter]
                            (watch letter example-child letter))
                          @letters)]
        {:value (str "Count: " @letters (into [] (map :value children)))
         :handle (fn [key]
                   (when (= (str (first key)) "+")
                     (swap! letters conj (str (second key))))
                   (doall (for [child children]
                            ((:handle child) key))))}))))

(comment

  (def app (example))

  (def c (example-child))

  (app)

  (c "f")

  (def _ ((:handle (app)) "+c"))
  (def _ ((:handle (app)) "c"))

  ((:handle (c "f")) "f"))
