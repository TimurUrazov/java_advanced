package info.kgeorgiy.ja.urazov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final static int QUEUE_SIZE_COEFFICIENT = 10;

    private final List<Thread> threadList;
    private final BlockingQueue blockingQueue;

    private boolean isClosed;

    /**
     * Creates an instance of {@code ParallelMapperImpl} with number of threads to process values
     *
     * @param threads number of threads to process values
     */
    public ParallelMapperImpl(final int threads) {
        blockingQueue = new BlockingQueue(threads * QUEUE_SIZE_COEFFICIENT);

        final Runnable workerTemplate = () -> {
            try {
                while (!Thread.interrupted()) {
                    blockingQueue.get().run();
                }
            } catch (InterruptedException ignored) {
                // Do nothing
            }
        };

        threadList = Stream.generate(() -> new Thread(workerTemplate))
                .limit(threads).peek(Thread::start).collect(Collectors.toList());
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args)
            throws InterruptedException {

        if (isClosed) {
            throw new IllegalStateException("Mapper has already been closed");
        }

        final TaskProcessor taskProcessor = new TaskProcessor(args.size(), Thread.currentThread());
        final List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));

        try {
            for (int i = 0; i < args.size(); i++) {
                final int indexToSet = i;
                blockingQueue.set(() -> {
                    if (!isClosed) {
                        try {
                            result.set(indexToSet, f.apply(args.get(indexToSet)));
                        } catch (final RuntimeException e) {
                            taskProcessor.processException(e);
                        }
                        taskProcessor.markCompletion();
                    } else {
                        taskProcessor.resetTasks();
                    }
                });
            }
        } catch (final InterruptedException e) {
            if (!isClosed) {
                throw e;
            }
        }

        taskProcessor.waitCompletion();

        return result;
    }

    /** Stops all threads. All unfinished mappings leave in undefined state. */
    @Override
    public void close() {
        if (!isClosed) {
            isClosed = true;
            synchronized (threadList) {
                if (isClosed) {
                    return;
                }
                threadList.forEach(Thread::interrupt);
            }
            ConcurrentUtils.joinResults(threadList);
            blockingQueue.reset();
        }
    }

    private static final class BlockingQueue {
        private final int limit;
        // :NOTE: собирать эксепшены из пользовательской функции
        private final Deque<Runnable> deque;

        private BlockingQueue(final int limit) {
            this.limit = limit;
            deque = new ArrayDeque<>();
        }

        private synchronized void set(Runnable value) throws InterruptedException {
            try {
                while (deque.size() >= limit) {
                    wait();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            deque.add(value);
            notifyAll();
        }

        private synchronized Runnable get() throws InterruptedException {
            while (deque.isEmpty()) {
                wait();
            }
            final Runnable runnable = deque.pop();
            notifyAll();
            return runnable;
        }

        private synchronized void reset() {
            deque.forEach(Runnable::run);
        }
    }

    private static final class TaskProcessor {
        private int tasksNum;
        private int counter;
        private boolean reset;

        private final ExceptionHandler<RuntimeException> exceptionHandler;
        private final Thread coordinator;

        private TaskProcessor(final int tasksNum, final Thread coordinator) {
            exceptionHandler = new ExceptionHandler<>();
            this.tasksNum = tasksNum;
            this.coordinator = coordinator;
        }

        private void resetTasks() {
            if (!reset) {
                reset = true;
                tasksNum = 0;
                coordinator.interrupt();
                exceptionHandler.setExceptions(
                        new IllegalStateException("Can't close mapper while mapping is not done")
                );
                notify();
            }
        }

        private void processException(final RuntimeException exception) {
            synchronized(exceptionHandler) {
                exceptionHandler.processException(exception);
            }
        }

        private synchronized void markCompletion() {
            counter++;
            if (completed()) {
                notify();
            }
        }

        private synchronized void waitCompletion() throws InterruptedException {
            try {
                while (!completed()) {
                    wait();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }

            exceptionHandler.drop();
        }

        private boolean completed() {
            return counter == tasksNum;
        }
    }
}
