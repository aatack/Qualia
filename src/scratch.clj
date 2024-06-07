(ns scratch)

(defn watcher []
  (let [cache (atom {})]
    (fn [key head & body]
      (when-not (contains? @cache key)
        (swap! cache assoc key (head)))
      (apply (get @cache key) body))))

(defn watch [mount & arguments]
  (let [children (atom {})
        inner-watch (fn [key child-mount & child-arguments]
                      ; This does not yet track which arguments the child was most
                      ; recently applied to
                      (when-not (contains? @children key)
                        (swap! children assoc key
                               (apply watch child-mount child-arguments)))
                      (get @children key))
        render (mount inner-watch)
        state (atom (apply render arguments))]
    (doall
     (for [dep (:deps @state)]
       (add-watch dep :k (fn [& args] (reset! state (apply render arguments))))))
    state))

(defn example-child [w]
  (let [count (atom 0)]
    (fn [target]
      {:value (str "Target " target " count " @count)
       :handle (fn [key]
                 (when (= key target)
                   (swap! count inc)))
       :deps [count]})))

(defn example [w]
  (let [letters (atom [])]
    (fn []
      (let [children (map (fn [letter]
                            (w letter example-child letter))
                          @letters)]
        {:value (str "Count: " @letters (into [] (map :value children)))
         :handle (fn [key]
                   (when (= (str (first key)) "+")
                     (swap! letters conj (str (second key))))
                   (doall (for [child children]
                            ((:handle child) key))))}))))

(comment

  (def x (watch example-child "c"))

  ((:handle @x) "c")

  @x

  @(reactive-atom example-child "a")

  (def app (example))

  (def c (example-child))

  (:value (app))

  (c "f")

  (def _ ((:handle (app)) "+c"))
  (def _ ((:handle (app)) "c"))

  ((:handle (c "f")) "f"))
