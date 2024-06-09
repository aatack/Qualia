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
                      ; recently applied to. This could be done by storing an atom of
                      ; arguments alongside each child
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

(defn mount [context component & arguments]
  (let [arguments-state (atom arguments)
        children-state (atom {})
        component-state (atom (component context))
        result-state ()]))


(def *path* (atom []))
(def *state* (atom {}))





(defn render [definition & arguments]
  (let [context (atom {:stack [] :state {}})
        state (fn [k] (get (get-in (:state @context) (:stack @context)) k))]))



(defn r [definition]
  (let [context (atom {})]
    (definition context)))


(defn recursive-example [x]
  (fn [context]
    (str x (recursive-example (dec x)))))



(defn example-child [context]
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



(defrecord Cursor [a k]
  clojure.lang.IDeref
  (deref [_]
    (get @a k))

  clojure.lang.IAtom
  (reset [_ new-value]
    (swap! a assoc k new-value))

  (swap [_ f]
    (swap! a update k f))
  (swap [_ f x]
    (swap! a update k f x))
  (swap [_ f x y]
    (swap! a update k f x y))
  (swap [_ f x y more]
    (swap! a update k #(apply f % x y more))))

(defn cursor [a k]
  (Cursor. a k))

(defn state [context key value]
  (swap! context (fn [current]
                   (if (contains? current key)
                     current
                     (assoc current key value))))
  (cursor context key))


(defn tracker [character]
  (fn [context]
    (let [count (state context :count 0)]
      {:value @count
       :handle (fn [key]
                 (when (= key character)
                   (swap! count inc)))})))




(comment

  (def t (tracker "c"))

  (def app-state (atom {}))

  (t app-state)

  (def _ ((:handle (t app-state)) "+c"))
  (def _ ((:handle (t app-state)) "c"))

  ((:handle (c "f")) "f"))
