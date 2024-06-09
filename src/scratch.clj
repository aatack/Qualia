(ns scratch
  (:require [state :refer [cursor]]))

(defn mount [component context]
  (let [value (cursor context :value)
        render (fn []
                 (reset! value (component context)))]
    (add-watch context :mount
               (fn [_ _ old new]
                 (when (or (not= (:state old) (:state new))
                           (not= (:children old) (:children new)))
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

;;   (-> app :value)

  (-> @c :value :value)

  (def _ ((:handle app) "d"))
  (def _ ((:handle app) "c"))

  ((:handle (c "f")) "f"))
