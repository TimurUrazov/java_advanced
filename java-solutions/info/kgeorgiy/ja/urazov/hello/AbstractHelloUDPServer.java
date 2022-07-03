package info.kgeorgiy.ja.urazov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static info.kgeorgiy.ja.urazov.hello.HelloUDPUtils.*;

public abstract class AbstractHelloUDPServer implements HelloServer {
    protected static final int DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 100;
    protected static final String RESPONSE_FORMAT = "Hello, %s";
    protected ExecutorService coordinator;

    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(final int port, final int threads) {
        coordinator = Executors.newSingleThreadExecutor();
        startImpl(port, threads);
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        closeImpl();
        shutdown(coordinator, DEFAULT_SHUTDOWN_TIMEOUT_MILLIS);
    }

    protected static void printException(final String message, final Exception e) {
        System.err.println(message + " " + e.getMessage());
    }

    protected static void start(final String[] args, final Supplier<HelloServer> server) {
        if (args == null || args.length != 2) {
            System.err.println("Usage:\t HelloUDPServer port threads");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Non-null arguments are required");
            return;
        }

        try (final HelloServer helloServer = server.get()) {
            helloServer.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (final NumberFormatException e) {
            System.err.println("Can not parse arguments");
        } catch (final HelloUDPException e) {
            System.err.println(e.getMessage());
        }
    }

    protected abstract void closeImpl();
    protected abstract void startImpl(final int port, final int threads);
}
