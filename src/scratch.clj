(ns scratch)

(defn example-child []
  (let [count (atom 0)]
    (fn [target]
      {:value (str "Target " target " count " @count)
       :handle (fn [key]
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
      (let [children (for [letter letters]
                       (watch letter example-child letter))]
        {:value (str "Count: " @letters)
         :handle (fn [key]
                   (when (= (str (first key)) "+")
                     (swap! letters conj (str (second key))))
                   (for [child children]
                     ((:handle child) key)))}))))

(comment

  (def app (example))

  (def c (example-child))

  (c "f")

  (app)

  ((:handle (app)) "c")
  ((:handle (c "f")) "f"))
