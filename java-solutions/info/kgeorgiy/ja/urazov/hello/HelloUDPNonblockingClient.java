package info.kgeorgiy.ja.urazov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.urazov.hello.HelloUDPUtils.*;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {
    @Override
    protected void runImpl(InetSocketAddress address, String prefix, int threads, int requests) {
        try (final Selector selector = Selector.open()) {
            IntStream.range(0, threads).forEach(i -> {
                try {
                    final DatagramChannel datagramChannel = DatagramChannel.open();
                    datagramChannel.configureBlocking(false);
                    try {
                        datagramChannel.register(
                                selector,
                                SelectionKey.OP_WRITE,
                                new HelloUDPClientChannelData(
                                        i, requests, datagramChannel.socket().getReceiveBufferSize()
                                )
                        );
                    } catch (SocketException e) {
                        throw new HelloUDPException("Error getting value of SO_RCVBUF", e);
                    }
                } catch (IOException e) {
                    throw new HelloUDPException("The channel could not be opened", e);
                }
            });

            while (!selector.keys().isEmpty() && !Thread.interrupted()) {
                if (selector.select(DEFAULT_REQUEST_TIMEOUT) != 0) {
                    for (final Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext();) {
                        final SelectionKey currentKey = it.next();

                        final HelloUDPClientChannelData data =
                                (HelloUDPClientChannelData) currentKey.attachment();

                        final DatagramChannel channel = (DatagramChannel) currentKey.channel();

                        final List<Integer> threadAndRequest = data.nextRequestNum();

                        if (currentKey.isWritable()) {
                            final ByteBuffer buffer = data.getContent();

                            final byte[] requestBytes = getBytes(
                                    REQUEST_FORMAT, prefix,
                                    threadAndRequest.get(0), threadAndRequest.get(1)
                            );

                            try {
                                buffer.clear();
                                buffer.put(requestBytes);
                                buffer.flip();
                                channel.send(buffer, address);
                            } catch (IOException e) {
                                throw new HelloUDPException("Error sending request", e);
                            }
                            currentKey.interestOps(SelectionKey.OP_READ);
                        }

                        if (currentKey.isReadable()) {
                            final ByteBuffer response = data.getContent();
                            response.clear();
                            channel.receive(response);
                            response.flip();

                            final String receivedText = decodeBytebuffer(response);

                            currentKey.interestOps(SelectionKey.OP_WRITE);

                            if (matches(
                                    receivedText, threadAndRequest.get(0), threadAndRequest.get(1)
                            )) {
                                System.out.println(receivedText);

                                data.incrementRequest();

                                if (data.limitAttained()) {
                                    try {
                                        currentKey.channel().close();
                                    } catch (IOException e) {
                                        throw new HelloUDPException("Can not close channel");
                                    }
                                    currentKey.cancel();
                                }
                            }
                        }

                        it.remove();
                    }
                } else {
                    selector.keys().forEach(k -> k.interestOps(SelectionKey.OP_WRITE));
                }
            }
        } catch (IOException e) {
            throw new HelloUDPException("Can not open selector", e);
        }
    }

    /**
     * The starting point of execution of {@link HelloUDPNonblockingClient}, which sends
     * UDP packets to server port of given host in non-blocking mode fixed times per thread
     * which number is given and contain given prefix and returns errors if they occurred
     *
     * @param args host port prefix threads requests, where
     *             host is server host,
     *             port is server port,
     *             prefix request prefix
     *             threads number of request threads
     *             requests number of requests per thread
     */
    public static void main(String[] args) {
        run(args, HelloUDPNonblockingClient::new);
    }
}
