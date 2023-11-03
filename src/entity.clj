(ns entity 
  (:require [clojure.set :as set]))

(defprotocol Entity
  (dependencies [this])
  (cursor? [this]))

(defrecord State [state]
  clojure.lang.IDeref
  (deref [_] state)

  Entity
  (dependencies [_] #{})
  (cursor? [_] true))

(defrecord Lookup [root path]
  clojure.lang.IDeref
  (deref [_] (get-in @root path))

  Entity
  (dependencies [this] (if (cursor? this)
                         (set (map #(concat % path) (dependencies root)))
                         (dependencies root)))
  (cursor? [_] (cursor? root)))

(defrecord Derive [function arguments]
  clojure.lang.IDeref
  (deref [_] (apply function (map deref arguments)))

  Entity
  (dependencies [_] (apply set/intersection (map dependencies arguments)))
  (cursor? [_] false))
