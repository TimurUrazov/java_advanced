package info.kgeorgiy.ja.urazov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.urazov.hello.HelloUDPUtils.*;

public class HelloUDPClient extends AbstractHelloUDPClient {
    private static final int DEFAULT_TIMEOUT_MILLIS = 200;

    @Override
    protected void runImpl(final InetSocketAddress destination, final String prefix,
                           final int threads, final int requests) {
        final ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        final IntFunction<Task> workerTemplate = thread -> () -> {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(DEFAULT_REQUEST_TIMEOUT);

                try {
                    final DatagramPacket request = getEmptyPacket(socket);
                    final DatagramPacket response = getEmptyPacket(socket);

                    IntStream.range(0, requests).forEach(requestNum -> {
                        final byte[] requestBytes = getBytes(REQUEST_FORMAT, prefix, thread, requestNum);

                        while (true) {
                            try {
                                fillPacket(request, requestBytes, destination);
                                socket.send(request);
                                socket.receive(response);

                                final String got = convertPacket(response);

                                if (matches(got, thread, requestNum)) {
                                    System.out.println(got);
                                    break;
                                }
                            } catch (SocketTimeoutException ignored) {
                                // Send till receive correct answer
                            } catch (IOException e) {
                                throw new HelloUDPException(
                                        "I/O exception occurred while receiving", e
                                );
                            }
                        }
                    });
                } catch (SocketException e) {
                    throw new HelloUDPException(
                            "Error getting value of SO_RCVBUF", e
                    );
                }
            } catch (SocketException e) {
                throw new HelloUDPException("The socket could not be bound or opened", e);
            }
        };

        final List<Task> workers = IntStream
                .range(0, threads)
                .mapToObj(workerTemplate)
                .collect(Collectors.toList());

        try {
            for (final Future<Void> future : threadPool.invokeAll(workers)) {
                future.get();
            }
            threadPool.shutdown();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdown(threadPool, DEFAULT_TIMEOUT_MILLIS);
        } catch (ExecutionException e) {
            throw new HelloUDPException("Error occurred while executing task in thread", e);
        }
    }

    private interface Task extends Callable<Void> {
        @Override
        default Void call() throws HelloUDPException {
            run();
            return null;
        }

        void run() throws HelloUDPException;
    }


    /**
     * The starting point of execution of {@link HelloUDPClient}, which sends
     * UDP packets to server port of given host in blocking mode fixed times per
     * thread which number is given and contain given prefix and returns errors
     * if they occurred
     *
     * @param args host port prefix threads requests, where
     *             host is server host,
     *             port is server port,
     *             prefix request prefix
     *             threads number of request threads
     *             requests number of requests per thread
     */
    public static void main(String[] args) {
        run(args, HelloUDPClient::new);
    }
}
