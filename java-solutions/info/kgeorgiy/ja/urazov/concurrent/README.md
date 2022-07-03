  ## Iterative parallelism

**Task**: 

1. Implement a class `IterativeParallelism`, which shall process the lists using multiple threads.
2. Methods: 
    - `minimum(threads, list, comparator)` -- first min value;
    - `maximum(threads, list, comparator)` -- first max value;
    - `all(threads, list, predicate)` -- check that all elements satisfy 
    `predicate`;
    - `any(threads, list, predicate)` -- check that some element satisfies 
    `predicate`
    - `filter(threads, list, predicate)`
    - `map(threads, list, function)`
    - `join(threads, list)`
3. `threads` parameter is the exact number of threads, that should be used to perform calculations.
4. _Concurrency Utilities_ are not allowed to use.
