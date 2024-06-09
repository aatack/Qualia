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
                 (println (not= (:value old) (:value new)) (:value old) (:value new))
                 (when (not= (:value old) (:value new))
                   ((:render @context)))))
    (mount builder @child-context)))

(def tracker-handler (memoize (fn [character count]
                                (fn [key]
                                  (when (= key character)
                                    (swap! count inc))))))

(defn tracker [character]
  (fn [context]
    (let [count (state context :count 0)]
      {:value @count
       :handle (tracker-handler character count)})))

(def tracker-pair-handler (memoize (fn [left-tracker-handler right-tracker-handler]
                                     (fn [key]
                                       (left-tracker-handler key)
                                       (right-tracker-handler key)))))

(defn tracker-pair [left-character right-character]
  (fn [context]
    (let [left-tracker (child context :left (tracker left-character))
          right-tracker (child context :right (tracker right-character))]
      {:value {left-character (:value left-tracker)
               right-character (:value right-tracker)}
       :handle (tracker-pair-handler (:handle left-tracker) (:handle right-tracker))})))

(defn build-context [] (atom {}))


(comment

  (def c (build-context))

  (def app (mount (tracker-pair "c" "d") c))
  (def app (mount (tracker "c") c))
  (def app (mount (tracker "d") c))

;;   (-> app :value)

  c

  (-> @c :value :value)

  (def _ ((:handle app) "d"))
  (def _ ((:handle app) "c"))

  ((:handle (c "f")) "f"))
