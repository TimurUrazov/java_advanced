package info.kgeorgiy.ja.urazov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static info.kgeorgiy.ja.urazov.hello.HelloUDPUtils.*;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    private static final int DEFAULT_CAPACITY_FACTOR = 4;
    private Selector selector;
    private ExecutorService service;
    private DatagramChannel datagramChannel;

    @Override
    protected void startImpl(final int port, final int threads) {
        service = Executors.newFixedThreadPool(threads);
        try {
            selector = Selector.open();
            try {
                datagramChannel = DatagramChannel.open();
                datagramChannel.bind(new InetSocketAddress(port));
                datagramChannel.configureBlocking(false);
                datagramChannel.register(
                        selector,
                        SelectionKey.OP_READ,
                        new HelloUDPServerChannelData(threads * DEFAULT_CAPACITY_FACTOR)
                );

                coordinator.execute(() -> {
                    try {
                        final ByteBuffer request = ByteBuffer.allocate(
                                datagramChannel.socket().getReceiveBufferSize()
                        );

                        while (selector.isOpen()) {
                            try {
                                try {
                                    final int numOfKeys = selector.select();
                                    if (numOfKeys == 0) {
                                        continue;
                                    }
                                } catch (final IOException e) {
                                    printException("Error waiting on selector", e);
                                }

                                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                                final SelectionKey currentKey = it.next();
                                it.remove();
                                final HelloUDPServerChannelData data =
                                        (HelloUDPServerChannelData) currentKey.attachment();

                                if (currentKey.isReadable()) {
                                    data.reserve();

                                    if (data.noDataRemaining()) {
                                        currentKey.interestOpsAnd(~SelectionKey.OP_READ);
                                    }

                                    try {
                                        final SocketAddress address = datagramChannel.receive(request);
                                        request.flip();
                                        final String receivedText = decodeBytebuffer(request);

                                        request.clear();

                                        service.execute(() -> {
                                            final byte[] msg = HelloUDPUtils.getBytes(
                                                    RESPONSE_FORMAT,
                                                    receivedText
                                            );
                                            final ByteBuffer buffer = ByteBuffer.wrap(msg);
                                            data.addResponse(address, buffer);
                                            if (selector.isOpen()) {
                                                currentKey.interestOpsOr(SelectionKey.OP_WRITE);
                                                selector.wakeup();
                                            }
                                        });
                                    } catch (IOException e) {
                                        printException("Error receiving request", e);
                                    }
                                } else if (currentKey.isWritable() && !data.noData()) {
                                    final ResponseData response = data.getResponse();

                                    if (!data.noDataRemaining()) {
                                        currentKey.interestOpsOr(SelectionKey.OP_READ);
                                        selector.wakeup();
                                    }

                                    if (data.noData()) {
                                        currentKey.interestOps(SelectionKey.OP_READ);
                                    }

                                    try {
                                        datagramChannel.send(
                                                response.buffer(),
                                                response.socketAddress()
                                        );
                                    } catch (IOException e) {
                                        printException("Error sending response", e);
                                    }
                                }
                            } catch (ClosedSelectorException ignored) {
                                // Do nothing
                            }
                        }
                    } catch (SocketException e) {
                        printException("Error getting value of SO_RCVBUF", e);
                    }
                });

            } catch (IOException e) {
                throw new HelloUDPException("The channel could not be opened", e);
            }
        } catch (IOException e) {
            throw new HelloUDPException("The selector could not be opened", e);
        }
    }

    @Override
    protected void closeImpl() {
        try {
            selector.close();
        } catch (IOException e) {
            printException("Can not close selector", e);
        }
        try {
            datagramChannel.close();
        } catch (IOException e) {
            printException("Can not close channel", e);
        }
        shutdown(service, DEFAULT_SHUTDOWN_TIMEOUT_MILLIS);
    }

    /**
     * The starting point of execution of {@link HelloUDPNonblockingServer}, which receives
     * UDP packets in non-blocking mode on given port by given number of threads, sends it back
     * and returns errors if they occurred
     *
     * @param args port threads, where
     *             port server port.
     *             threads number of working threads.
     */
    public static void main(String[] args) {
        start(args, HelloUDPNonblockingServer::new);
    }
}
