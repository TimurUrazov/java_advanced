package info.kgeorgiy.ja.urazov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractHelloUDPClient implements HelloClient {
    /**
     * Default timeout for waiting on selector or socket timeout
     */
    protected static final int DEFAULT_REQUEST_TIMEOUT = 100;

    /**
     * Format of request sending by channel or socket
     */
    protected static final String REQUEST_FORMAT = "%s%d_%d";

    /**
     * Format of response receiving from channel or socket
     */
    protected static final String RESPONSE_FORMAT = "\\D*%d\\D+%d\\D*";

    /**
     * Runs Hello client.
     * This method should return when all requests completed.
     *
     * @param host server host
     * @param port server port
     * @param prefix request prefix
     * @param threads number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(final String host, final int port,
                    final String prefix, final int threads,
                    final int requests) {
        try {
            final InetAddress address = InetAddress.getByName(host);
            final InetSocketAddress destination = new InetSocketAddress(address, port);

            runImpl(destination, prefix, threads, requests);

        } catch (UnknownHostException e) {
            throw new HelloUDPException("Host is unknown", e);
        }
    }

    /**
     * Checks whether given sample matches expected string satisfying format
     *
     * @param got sample for comparison
     * @param thread number of thread
     * @param request number of request
     * @return status whether sample and expected string are equal
     */
    protected static boolean matches(final String got, final int thread, final int request) {
        return got.matches(String.format(RESPONSE_FORMAT, thread, request));
    }

    /**
     * Checks whether args satisfy input format and create client according to the given arguments
     *
     * @param args arguments to create client
     * @param client client supplier
     */
    protected static void run(final String[] args, final Supplier<HelloClient> client) {
        if (args == null || args.length != 5) {
            System.err.println("Usage:\t HelloUDPClient host port prefix threads requests");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Non-null arguments are required");
            return;
        }

        try {
            client.get().run(
                    args[0], Integer.parseInt(args[1]), args[2],
                    Integer.parseInt(args[3]), Integer.parseInt(args[4])
            );
        } catch (NumberFormatException e) {
            System.err.println("Can not parse input numbers");
        } catch (HelloUDPException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Implementation detail of Hello client runner
     *
     * @param destination destination address to send requests
     * @param prefix request prefix
     * @param threads number of request threads
     * @param requests number of requests per thread
     */
    protected abstract void runImpl(final InetSocketAddress destination,
                                    final String prefix,
                                    final int threads,
                                    final int requests);
}
