(ns entity
  (:require [clojure.set :as set]))

(defprotocol Entity
  "Describes any value that is derived in some way from a fixed state object.
   
   Entities track which paths within the state object they are derived from.  The set
   of dependencies describes the set of paths within the state which, should any of
   their values change, might cause the value of the entity to also change."

  (dependencies [this]
    "Return the paths of all entities that are dependencies of this entity.")

  (lookup? [this]
    "Determine whether or not this is a direct lookup from the underlying state."))

(defrecord Database [value]
  clojure.lang.IDeref
  (deref [_] value)

  Entity
  (dependencies [_] #{})
  (lookup? [_] true))

(defn database
  "Wrap a value in a database, making it accessible to entity functions."
  [value]
  (->Database value))

(defrecord Select [entity path]
  clojure.lang.IDeref
  (deref [_] (get-in @entity path))

  Entity
  (dependencies [this] (if (lookup? this)
                         (set (map #(concat % path) (dependencies entity)))
                         (dependencies entity)))
  (lookup? [_] (lookup? entity)))

(defn select
  "An entity retrieving another value from a database and accesesing a path within it."
  [entity & path]
  (->Select entity path))

(defrecord Compute [function arguments]
  ;; (TODO) Consider adding an optional inverse function, which can be used to set the
  ;; underlying state where an inverse conversion is possible

  clojure.lang.IDeref
  (deref [_] (apply function (map deref arguments)))

  Entity
  (dependencies [_] (apply set/intersection (map dependencies arguments)))
  (lookup? [_] false))

(defn compute
  "Compute a new value from a set of entities."
  [function & arguments]
  (->Compute function arguments))
