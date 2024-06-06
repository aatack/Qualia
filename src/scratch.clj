(ns scratch)


(defn example []
  (let [letters (atom [])
        cache (atom {})]
    (fn []
      (let [children []]
        {:value (str "Count: " @letters)
         :handle (fn [key]
                   (when (= (str (first key)) "+")
                     (swap! letters conj (str (second key))))
                   (for [child children]
                     ((:handle child) key)))}))))

(defn example-child []
  (let [count (atom 0)]
    (fn [target]
      {:value (str "Target " target " count " @count)
       :handle (fn [key]
                 (when (= key target)
                   (swap! count inc)))})))

(comment

  (def app (example))

  (def c (example-child))

  (c "f")

  (app)

  ((:handle (app)) "+c")
  ((:handle (c "f")) "f"))
