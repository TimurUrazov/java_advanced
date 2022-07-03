package info.kgeorgiy.ja.urazov.hello;

/**
 * Exception occurred while sending queries from {@link HelloUDPClient}
 * or receiving them by {@link HelloUDPServer}.
 */
public class HelloUDPException extends RuntimeException {
    /**
     * Creates an instance of {@code HelloUDPException} with given message
     *
     * @param message given message
     */
    HelloUDPException(final String message) {
        super(message);
    }

    /**
     * Creates an instance of {@code HelloUDPException} with given message and exception
     *
     * @param message given message
     * @param exception given exception
     */
    HelloUDPException(final String message, final Exception exception) {
        super(message + ": " + exception.getMessage());
    }
}
