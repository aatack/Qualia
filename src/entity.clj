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

(defrecord Lookup [root path]
  clojure.lang.IDeref
  (deref [_] (get-in @root path))

  Entity
  (dependencies [this] (if (lookup? this)
                         (set (map #(concat % path) (dependencies root)))
                         (dependencies root)))
  (lookup? [_] (lookup? root)))

(defrecord Derive [function arguments]
  clojure.lang.IDeref
  (deref [_] (apply function (map deref arguments)))

  Entity
  (dependencies [_] (apply set/intersection (map dependencies arguments)))
  (lookup? [_] false))
