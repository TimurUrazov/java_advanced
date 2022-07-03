package info.kgeorgiy.ja.urazov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;
    private final List<Thread> threadList;

    /**
     * Creates an instance of {@code IterativeParallelism}
     * with {@code ParallelMapper} which will be used for mapping functions
     *
     * @param parallelMapper {@code ParallelMapper} for mapping functions
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this(null, parallelMapper);
    }

    /**
     * Creates an instance of {@code IterativeParallelism}
     */
    public IterativeParallelism() {
        this(new ArrayList<>(), null);
    }

    private IterativeParallelism(final List<Thread> threadList, final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
        this.threadList = threadList;
    }

    /**
     * Returns maximum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return maximum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {

        if (values.isEmpty()) {
            throw new NoSuchElementException("No value presents");
        }

        return parallelProcessing(threads, values, Function.identity(), () -> null,
                BinaryOperator.maxBy(Comparator.nullsFirst(comparator)));
    }

    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param values values to join.
     *
     * @return list of joined result of {@link #toString()} call on each value.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelProcessing(threads, values, x -> new StringBuilder(x.toString()),
                StringBuilder::new, StringBuilder::append).toString();
    }

    /**
     * Filters values by predicate.
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param predicate filter predicate.
     *
     * @return list of values satisfying given predicated. Order of values is preserved.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values,
                              Predicate<? super T> predicate) throws InterruptedException {
        return parallelProcessing(threads, values, x -> predicate.test(x) ? 1 : 0, Function.identity());
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param f mapper function.
     *
     * @return list of values mapped by given function.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values,
                              Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelProcessing(threads, values, x -> 1, f);
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return parallelProcessing(threads, values, Function.identity(), monoid);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param lift mapping function.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values,
                              Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return parallelProcessing(threads, values, lift, monoid);
    }

    /**
     * Returns minimum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get minimum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return minimum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfy predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether all values satisfy predicate or {@code true}, if no values are given
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelProcessing(threads, values, predicate::test, () -> Boolean.TRUE,
                Boolean::logicalAnd);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether any value satisfies predicate or {@code false}, if no values are given
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }


    // -------------- private section ------------------

    private <T, U> U parallelProcessing(final int threads,
                                        final List<? extends T> values,
                                        final Function<T, U> extractor,
                                        final Supplier<U> identity,
                                        final BinaryOperator<U> operator) throws InterruptedException {
        final int chunkSize = Math.max(values.size() / threads, 1);
        final int threadsNum = Math.min(threads, values.size());

        final List<Worker<T, U>> workerList = new ArrayList<>(threadsNum);

        final List<List<? extends T>> chunks = new ArrayList<>(threadsNum);

        final int overflown = values.size() - threadsNum * chunkSize;

        for (int i = 0, offset = 0; i < threadsNum; i++) {
            chunks.add(values.subList(offset, offset += (i < overflown ? 1 : 0) + chunkSize));
        }

        final BiFunction<U, T, U> processingFunc = (x, y) -> operator.apply(x, extractor.apply(y));

        if (parallelMapper == null) {
            final Consumer<List<? extends T>> listConsumer = chunk -> {
                final Worker<T, U> worker = new Worker<>(chunk,
                        processingFunc, identity.get());

                final Thread thread = new Thread(worker);
                threadList.add(thread);
                workerList.add(worker);
                thread.start();
            };

            // :NOTE: вынести Runnable
            chunks.forEach(listConsumer);

            final ExceptionHandler<InterruptedException> exceptionHandler = new ExceptionHandler<>();

            ConcurrentUtils.joinResults(threadList, exceptionHandler);

            exceptionHandler.drop();

            return processChunk(workerList.stream().map(Worker::getResult).collect(Collectors.toList()),
                    operator, identity.get());
        }

        return processChunk(parallelMapper.map(
                list -> processChunk(list, processingFunc, identity.get()), chunks),
                operator, identity.get());
    }

    private <T, U> List<U> parallelProcessing(final int threads,
                                              final List<? extends T> values,
                                              final Function<? super T, Integer> size,
                                              final Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelProcessing(threads, values,
                x -> new ArrayList<>(Collections.nCopies(size.apply(x), f.apply(x))),
                ArrayList::new, (x, y) -> {
                    x.addAll(y);
                    return x;
                });
    }

    public <T, R> R parallelProcessing(final int threads,
                                       final List<T> values,
                                       final Function<T, R> function,
                                       final Monoid<R> monoid) throws InterruptedException {
        return parallelProcessing(threads, values, function, monoid::getIdentity, monoid.getOperator());
    }

    private <T, U> U processChunk(final List<? extends T> values,
                                  final BiFunction<U, T, U> operator,
                                  U result) {
        for (final T value : values) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            result = operator.apply(result, value);
        }
        return result;
    }

    private final class Worker<T, U> implements Runnable {
        private final BiFunction<U, T, U> operator;
        private final List<? extends T> values;
        private U result;

        private Worker(final List<? extends T> values,
                       final BiFunction<U, T, U> operator,
                       final U identity) {
            this.values = values;
            this.operator = operator;
            this.result = identity;
        }

        @Override
        public void run() {
            result = processChunk(values, operator, result);
        }

        private U getResult() {
            return result;
        }
    }
}
