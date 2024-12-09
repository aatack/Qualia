(ns entities
  (:require [state.core :as q]))

(q/let-state [x 1]
  @x)
