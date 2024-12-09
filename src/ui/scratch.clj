(ns ui.scratch
  (:require
   [quil.core :as q]
   [state.core :as s]))

(s/defentity debug-entity [bounds mouse buttons keys]
  (let [[x y] @mouse
        in-bounds (< x 100)]

    (s/let-entity [inb (s/on-change (< x 100) (fn [] (println "Changed")))]
      @inb)

    #_(s/when-changed :in-bounds in-bounds
                      (println "In bounds? " in-bounds))

    (str x " " y " " in-bounds)))

(defn start! [entity title [width height]]

  (s/let-state [bounds [width height]
                mouse [0 0]
                buttons #{}
                keys #{}]

    (s/let-entity [app (entity bounds mouse buttons keys)]

      (letfn [(draw []
                (q/background 0)
                (q/text-size 32)
                (q/text-align :left :top)
                (q/fill 255 255 255)
                (q/no-stroke)
                (q/text (str @app) 0 0))

              (key-pressed [])

              (key-released [])

              (mouse-pressed [])

              (mouse-released [])

              (mouse-moved []
                ;; Also needs to trigger on drag - doesn't update if a button is held
                (mouse [(q/mouse-x) (q/mouse-y)]))]

        (q/sketch :title title
                  :size [width height]
                  :draw draw
                  :key-pressed key-pressed
                  :key-released key-released
                  :mouse-pressed mouse-pressed
                  :mouse-released mouse-released
                  :mouse-moved mouse-moved)))))

(comment

  (start! debug-entity "Debug" [1800 800]))
