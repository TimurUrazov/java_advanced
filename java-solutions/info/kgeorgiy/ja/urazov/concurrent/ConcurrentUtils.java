package info.kgeorgiy.ja.urazov.concurrent;

import java.util.List;

public class ConcurrentUtils {
    /**
     * Joins threads from given list of threads and collects errors that led to interruption
     * to exceptionHandler if it is not null
     *
     * @param threadList treads to be joined
     * @param exceptionHandler collect exceptions which led to interruption of the thread
     *                         who created threads in treadList
     */
    public static void joinResults(final List<Thread> threadList,
                                   final ExceptionHandler<InterruptedException> exceptionHandler) {
        boolean interrupted = false;

        for (int i = 0; i < threadList.size(); i++) {
            try {
                threadList.get(i).join();
            } catch (final InterruptedException e) {
                if (!interrupted) {
                    interrupted = true;
                    for (int j = i; j < threadList.size(); j++) {
                        threadList.get(j).interrupt();
                    }
                }
                if (exceptionHandler != null) {
                    exceptionHandler.processException(e);
                }
                i--;
            }
        }
    }

    /**
     * Joins threads from given list of threads
     *
     * @param threadList treads to be joined
     */
    public static void joinResults(final List<Thread> threadList) {
        joinResults(threadList, null);
    }
}
