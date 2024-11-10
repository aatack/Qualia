(ns state)

(defrecord Cursor [a k]
  clojure.lang.IDeref
  (deref [_]
    (get @a k))

  clojure.lang.IAtom
  (reset [_ new-value]
    (get (swap! a assoc k new-value) k))

  (swap [_ f]
    (get (swap! a update k f) k))
  (swap [_ f x]
    (get (swap! a update k f x) k))
  (swap [_ f x y]
    (get (swap! a update k f x y) k))
  (swap [_ f x y more]
    (get (swap! a update k #(apply f % x y more)) k)))

(defn cursor [a k]
  (Cursor. a k))

(defrecord Context [refresh
                    arguments
                    state
                    children
                    current
                    provides
                    consumes
                    handler])

(defrecord ComponentRun [result
                         state
                         children
                         consumed
                         handler])