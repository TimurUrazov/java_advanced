package info.kgeorgiy.ja.urazov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static info.kgeorgiy.ja.urazov.hello.HelloUDPUtils.*;

public class HelloUDPServer extends AbstractHelloUDPServer {
    private static final int DEFAULT_QUEUE_SIZE = 100000;
    private static final long DEFAULT_KEEP_ALIVE_TIME = 500L;
    private ThreadPoolExecutor service;
    private DatagramSocket socket;
    private boolean loaded;

    @Override
    protected void startImpl(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            service = new ThreadPoolExecutor(
                    threads, threads,
                    DEFAULT_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(DEFAULT_QUEUE_SIZE)
            );

            final Lock lock = new ReentrantLock();
            final Condition unloaded = lock.newCondition();

            coordinator = Executors.newSingleThreadExecutor();

            coordinator.execute(() -> {
                try {
                    while (!socket.isClosed()) {
                        final DatagramPacket packet = getEmptyPacket(socket);

                        try {
                            socket.receive(packet);

                            try {
                                service.execute(() -> {
                                    try {
                                        fillPacket(
                                                packet,
                                                getBytes(RESPONSE_FORMAT, convertPacket(packet)),
                                                packet.getSocketAddress()
                                        );

                                        socket.send(packet);

                                        if (loaded
                                                && service.getQueue().size() < 2 * DEFAULT_QUEUE_SIZE / 3
                                                && lock.tryLock()) {
                                            unloaded.signal();
                                            lock.unlock();
                                        }
                                    } catch (IOException e) {
                                        printException("I/O exception occurred while sending", e);
                                    }
                                });
                            } catch (RejectedExecutionException e) {
                                loaded = true;
                                try {
                                    lock.lock();
                                    unloaded.await();
                                } catch (InterruptedException ignored) {
                                    // Do nothing
                                } finally {
                                    lock.unlock();
                                }
                                loaded = false;
                            }
                        } catch (IOException e) {
                            if (!socket.isClosed()) {
                                printException("Can not receive packet", e);
                            }
                        }
                    }
                } catch (SocketException e) {
                    printException("Error getting value of SO_RCVBUF", e);
                }
            });
        } catch (SocketException e) {
            throw new HelloUDPException("The socket could not be bound or opened", e);
        }
    }

    @Override
    protected void closeImpl() {
        socket.close();
    }

    /**
     * The starting point of execution of {@link HelloUDPServer}, which receives
     * UDP packets in blocking mode on given port by given number of threads, sends it back
     * and returns errors if they occurred
     *
     * @param args port threads, where
     *             port server port.
     *             threads number of working threads.
     */
    public static void main(String[] args) {
        start(args, HelloUDPServer::new);
    }
}
