- A component is given a completely unregulated "workspace"
- When some part of the global workspace changes, the component is asked to update its workspace
- The function for doing this looks something like `(update [workspace context arguments paths-in paths-out] ...)`
- This extremely general wrapper should allow you to write heavily optimised components if you really want to
- Most typical components should - hopefully - become quite simple under this paradigm
- We could impose some structure upon the returned workspace to standardise some common optimisations
- For example, if the workspace always contains `:arguments`, `:state`, and `:consumed` forms, and there's no global state (only provided state), you know that you don't need to do any updates at all if:
  - All arguments are the same
  - All of your consumed paths, and those of your children, diverge from all of the updated paths
    - Is this horrendously difficult to check, computationally?
    - Because there'll probably be lots of nice branching behaviour, seems like it'd be pretty easy to check if we really wanted to find an optimised way of writing it (with some custom algorithm)
  - Also, at each stage you can cull the dictionary of changed paths according to the dictionary of consumed paths, which makes future checks (further down the line) easier
- The output of the update method should be a series of path-value pairs, which are used by the framework to update its state atom externally
  - This also means that callers of child components can determine whether or not they need to update any of their keys, or whether they can just return the update as-is
- Another nice thing is that the entire tree - including all globally- and locally-managed state - can be viewed as a single clojure object, with no extra bells or whistles
- Finally, it would be easy to come up with some heuristic to work out when to "collapse" the tree of dependencies
  - Eg. if a component is listening to the paths `[:state :text]`, `[:state :history :1]`, and `[:state :start]`, it might make sense to collapse that to just `[:state]` to reduce the complexity of checks
  - If you wanted to impose a little more structure, you could say that you're going to represent paths with two components: the component path and the state path. Then you'd know that you don't ever need to consider any updates that have diverging component paths, since there's no way for a component to affect another that isn't in the same branch
    - Though I don't like this as much because it's kind of cool that the state of a children of a component exist exclusively inside its workspace, and aren't in any way special; they're the same as any other state in the tree
- Obviously you'd need a bunch of wrappers around all of this to make it usable anyway
- Another cool thing is that you really can go all the way on the "events as state" train, even for impulse events like clicks and key presses
  - You'd just make sure there's a "key press" path, and every time it gets an updated signal you assume its current value is the key pressed
  - To save on rendering time, if multiple come through at once you could send down the events without a `:render` request in the `paths-out`, so highly-optimised components could even avoid rendering themselves until actually requested if they really wanted to
  - One important convention for this to actually work: any values of `nil` in the returned mapping (paths out) must always be interpreted as "computed lazily; must be requested specifically to obtain a value"
- [ ] A conundrum: if the mouse position is to be constantly in the state, and always updated, it mustn't trigger spurious updates whenever it moves
  - This means that components need to be able to say whether they use the mouse status, to prevent common components like `translate` from having to continually update the mouse position when their children are uninterested in the mouse position
  - Crucially, you need to know whether to compute the new translated mouse position before rendering any children (as you need to build the context); but you don't know whether or not the child is consuming the mouse position until after it's been rendered - and hence until after you've already computed the new position
  - I'm sure you could probably come up with a contrived example where the child becomes dependent on the mouse position suddenly, as a result of a change in some other variable
    - Actually I don't even think it would need to be all that contrived...
  - [ ] Are lazy contexts the way to solve this?
    - Importantly: while I suspect it's possible for a child component to start caring about whether the mouse position _will change_ spontaneously, I don't think it's possible for it to suddenly start caring about whether it _has changed_, since it didn't know what the previous value was in the first place
- It's interesting to note that literally none of this is UI-specific - at least, not in its most general case
  - Surely this is reinventing some wheel somewhere?
  - I suppose what it really is is a way of laying out the classic observer architecture in a way that (sort of?) guarantees there are no loops
- [ ] Another interesting unanswered question: is it important for this framework to be able to handle state-change-induced state changes?
  - Example: a component is added. It then becomes focused straight away. If the focus is maintained at the top level, how does the _resolution_ of a change in state propagate through to then make its own change in the state?
  - A naive solution would be to say that whichever change actually prompted the state change should also have set the focus, but this is shortsighted: it potentially had no way of knowing what the focus should be set to
  - Then again, the beauty of having the whole tree at your disposal is that it might be possible to easily identify - and surgically manipulate - other parts of the tree, even though they're in entirely separate branches
- Note that each value in the context needs to come with some indication of where it came from
  - Ie. if a component depends on a value from the context, upon which path should it then depend?
- [ ] Need to work out which problem this whole thing actually solves
  - Ie. what are the guiding principles of this system?
- Thoughts on more concrete data representations:
  - Paths should be represented as referenceable objects, which contain a path and the most up-to-date state object
    - Each component could be passed its workspace as a path, which reduces the amount of information you need to pass to the update function in order to let the component know where it "lives"
    - It also makes it easier to get the initial "pointer" to pass to any children - ie. you can just extend the pointer you've been given for your workspace
    - ~~Something else here I've forgotten...~~ You can then return a bunch of these objects from the child when it's been run (or, better yet, store them as a member of the child's workspace) and join them all together to be sent up to the next level
    - Possible name for these objects: "entities". This makes sense as they refer to a particular part of the state
  - How about representing the context as a bundle of entities as well?
    - I don't see why not
    - It gets a bit more complicated in two scenarios I can think of:
      - Derived context that isn't necessarily stored anywhere, like the updated bounds passed on by a translate component
      - Global state that's stored in the context, and for which a more deeply nested part is then accessed
    - Both of the above concerns can be solved by making entities a bit more like a traditional observable, but making them stateless; ie. instead of always just maintaining a path, they can be asked to return a set of dependent paths
      - The above scenarios are solved as:
        - Derived entities can simply refer to the original entities from which they were derived
        - Global state, if stored in an entity, can simply have a cursor called on it (as any other ~~piece of state~~ entity would) to select only the part that's actually used as a dependency
      - Ultimately I suppose these should have test cases that run for them
- [ ] Should the default interface have `update` return a set of changed paths within the workspace?
  - This might be useful for avoiding further recomputation in the parent if a particular object hasn't been altered
  - On the other hand, it might be an enormous faff for not much gain at all
  - ==> Let's not do it like that for now, and revisit later if optimisation proves that's actually a bottleneck
- Thoughts on optimising the path intersection algorithm:
  - The chances are that it doesn't need to be optimised for a while
  - By far the most common operation will be taking two sets of entities - one representing changes and one dependencies - and working out whether they "overlap"
    - In this context, overlapping means that they don't diverge. So, `[:a :b]` overlaps with both `[:a]` and `[:a :b :c]`, but not `[:a :c]`
  - A set of paths can be reduced to a nested map; the leaves would just be any non-map object. So the set of paths `#{[:a :b]}` would be represented as `{:a {:b true}}`
    - [ ] When reducing sets that have overlapping paths (`#{[:a :b :c] [:a :b]}`) I think it's fine to just take the longer one? So this would reduce to `{:a {:b {:c true}}}`
  - Comparing two nested maps then becomes a simple recursive operation:
    - Find the intersection of their keys
    - If the intersection is empty, there's no overlap
    - For each joined key, compare the two corresponding values
      - If either is `true`, there's an overlap
      - Otherwise, you have two maps, and can apply the algorithm recursively again
  - I think this algorithm would work and exit early most of the time?
- Possible interface for higher-level (ie. unoptimised) components:

  ```clojure
  (defcomponent example
    [a "Must be passed as an argument"
     b (default 10) "Will default to `10` if not passed"
     c (context :state :cache 1) "Derives its value from the component's context"
     d (context :mouse) (default [0 0]) "Derived from the context, or just a default"]

    bounds
    (if-let [[mouse-x mouse-y] d]
      [(* mouse-x 2) (* mouse-y 2)]
      [100 100])

    render
    (rectangle bounds [0.2 0.6 0.7]))
  ```

  - Calling this component would look like `(example 1 :c 2)`
  - The body of the component is specified as a bunch of key-value pairs, where each key maps to the form that will be evaluated to determine its result
  - Only the requested results will be evaluated, meaning some components will be able to provide a quicker return if eg. only their bounds are requested, and not the render
  - If there are an odd number of forms, the odd one out will have its results used instead of any that are not specified
    - That way it's also allowed to simply call some of the lower-level components, but override one or two of the resulting values
  - Each argument would get passed as an entity that can be referenced
    - Technically some of them can also be set, though that should only really be happening in callbacks (ie. updating the state shouldn't really trigger more changes to the state)
  - [ ] How will this deal with splatted arguments?
    - One option: separate the passed arguments and context-derived arguments
      - I don't like this as it would be incredibly powerful to be able to sometimes specify arguments and sometimes let the component handle them itself
    - Another option: introduce some special notation for it
      - This is difficult as then questions are raised about how to bind those values in the presence or absence of other optional arguments
        - We do need to solve this problem anyway, though, so it might still be fine
