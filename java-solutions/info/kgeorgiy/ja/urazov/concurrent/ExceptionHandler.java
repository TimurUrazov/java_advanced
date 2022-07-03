package info.kgeorgiy.ja.urazov.concurrent;

public class ExceptionHandler<T extends Throwable> {
    private T exceptions;

    /**
     * Creates an instance of {@code ExceptionHandler}
     */
    public ExceptionHandler() {
        exceptions = null;
    }

    /**
     * Returns exceptions that a handler contains
     */
    public T getExceptions() {
        return exceptions;
    }

    /**
     * Process a given exception: if handler doesn't contain an exception, that it sets it,
     * otherwise it suppresses it and add to those that already exists
     */
    public void processException(T exception) {
        if (this.exceptions == null) {
            this.exceptions = exception;
        } else {
            this.exceptions.addSuppressed(exception);
        }
    }

    /**
     * Check if handler contains exceptions
     *
     * @return {@code true} if handler contains exceptions, false - otherwise
     */
    public boolean empty() {
        return exceptions == null;
    }

    /**
     * Throws collected exceptions if they exist
     *
     * @throws T collected exceptions to be thrown
     */
    public void drop() throws T {
        if (!empty()) {
            throw exceptions;
        }
    }

    /**
     * Sets an exception
     *
     * @param exception exception to be set
     */
    public void setExceptions(T exception) {
        exceptions = exception;
    }
}
