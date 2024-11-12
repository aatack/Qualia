(ns scratch
  (:require [state :refer [cursor]]))

(defn mount [component context]
  (let [value (cursor context :value)
        render (fn []
                 (reset! value (component context)))]
    (swap! context assoc :render render)
    (add-watch context :mount
               (fn [_ _ old new]
                 (when (not= (:state old) (:state new))
                   (render))))
    (render)))

(defn state [context key value]
  (swap! context (fn [current]
                   (if (contains? (:state current) key)
                     current
                     (assoc-in current [:state key] value))))
  (-> context
      (cursor :state)
      (cursor key)))

(defn child [context key builder]
  (let [child-context (-> context (cursor :children) (cursor key))]
    (swap! child-context (fn [current] (or current (atom {}))))
    (add-watch @child-context :parent
               (fn [_ _ old new]
                 (when (not= (assoc (:value old) :handle nil)
                             (assoc (:value new) :handle nil))
                   ((:render @context)))))
    (mount builder @child-context)))

(defn tracker [character]
  (fn [context]
    (let [count (state context :count 0)]
      {:value @count
       :handle (fn [key]
                 (when (= key character)
                   (swap! count inc)))})))

(defn tracker-pair [left-character right-character]
  (fn [context]
    (let [left-tracker (child context :left (tracker left-character))
          right-tracker (child context :right (tracker right-character))]
      {:value {left-character (:value left-tracker)
               right-character (:value right-tracker)}
       :handle (fn [key]
                 ((:handle left-tracker) key)
                 ((:handle right-tracker) key))})))

(defn build-context [] (atom {}))


(comment

  (def c (build-context))

  (def app (mount (tracker-pair "c" "d") c))
  (def app (mount (tracker "c") c))
  (def app (mount (tracker "d") c))

  (-> @c :value :value)

  (def _ ((:handle app) "d"))
  (def _ ((:handle app) "c"))

  ((:handle (c "f")) "f"))

(defn q-internal [values builder]
  (fn [state updates context]
    ()))

(defn q-contextual [values builder]
  (fn [state updates context]
    ()))

(defn q-nested [entities builder]
  (fn [state updates context]
    ()))

(defn q-literal [value]
  (fn [state updates context]
    (assoc state :value value)))

(defn example-internal [x y]
  (q-internal {:x x :y y}
              (fn [values] (str (:x values) (:y values)))))

(defn example-contextual [a]
  (q-contextual {:x 1}
                (str (example-internal 1 2) a)))

(defn example-nested [f g]
  (q-nested {:a (example-internal 1 2)
             :b (example-contextual "test")}
            (fn [values] (str (:a values) (:b values)))))
