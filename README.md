# Qualia

A proof-of-concept system for computing data from stored state, then running updates to that state and propagating any required changes throughout.
Broadly this works in a similar way to React, except it can be used for any arbitrary data structure rather than being tied to the DOM.

## Example

```clojure
(require '[macros :refer [defentity let-internal let-context def-context let-nested]]
         '[runner :refer [build-runner]])

(defentity counter [name]
  (let-internal [count 0]
                (let-context [font]
                             [:on-click
                              (fn [] (swap! count inc))
                              [:text (str name ": " @count) font]])))

(defentity counters []
  (def-context [font "arial"]
    (let-nested [left (counter "Left")
                 right (counter "Right")]
                [:row left right])))

(def runner (build-runner (counters)))

@runner
;; [:row
;;  [:on-click #function[...] [:text "Left: 0" "arial"]]
;;  [:on-click #function[...] [:text "Right: 0" "arial"]]]

;; Access and call the increment function for the left counter.  The right counter's
;; value will not be recomputed
((get-in @runner [1 1]))

@runner
;; [:row
;;  [:on-click #function[...] [:text "Left: 1" "arial"]]
;;  [:on-click #function[...] [:text "Right: 0" "arial"]]]
```

## Tests

For some reason, the test runner wasn't playing well with VSCode.
The tests have therefore been left in comments at the bottom of each file, and need to be run manually.

## Performance

This falls somewhere in the middle of the spectrum between simplicity and performance, with the ability to do some caching - not every change triggers every piece of data to be recomputed - but with only a few atomic constructs and under 250 lines of code, excluding tests.
This should be a reasonable tradeoff, allowing Qualia to be used for most standard UI purposes without undue performance concerns; but it may fall down on more intensive applications.

A couple of known performance bottlenecks:

- There's no way of doing pre-checks on contextual state, which means all entities consuming a particular key from the context will be recomputed every time that key is updated; even if the change is not relevant to that entity
- If a nested entity is recomputed, the parent will be recomputed no matter what, even if the value of the nested entity does not change - for example, if its internal state changes
- Caching will not work for entities that take anonymous functions as arguments, since two instances of the same anonymous function will never be considered equal, even if their behaviour is identical
