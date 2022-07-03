package info.kgeorgiy.ja.urazov.hello;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HelloUDPUtils {
    /**
     * Returns new UDP packet having size available by given socket
     *
     * @param socket given socket
     * @return new UDP packet
     * @throws SocketException if
     */
    public static DatagramPacket getEmptyPacket(final DatagramSocket socket) throws SocketException {
        final int receiveBufferSize = socket.getReceiveBufferSize();
        return new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
    }

    /**
     * Returns string that given UDP packet contains in UTF-8 charset
     *
     * @param packet given UDP packet
     * @return string in packet
     */
    public static String convertPacket(DatagramPacket packet) {
        return new String(
                packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8
        );
    }

    /**
     * Fill existing UDP packet with data from given byte array and given socket address
     *
     * @param packet given UDP packet
     * @param bytes given byte array
     * @param address socket address
     */
    public static void fillPacket(final DatagramPacket packet, final byte[] bytes,
                                  final SocketAddress address) {
        packet.setData(bytes);
        packet.setSocketAddress(address);
    }

    /**
     * Returns byte array of string satisfying given format
     *
     * @param format given string format
     * @param text first argument for format
     * @param numbers optional argument for format
     * @return bytes of string satisfying given format
     */
    public static byte[] getBytes(final String format, final String text, final int ...numbers) {
        final String str;
        if (numbers.length == 0) {
            str = String.format(format, text);
        } else {
            str = String.format(format, text, numbers[0], numbers[1]);
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Shuts down given executor service and awaits given timeout
     *
     * @param threadPool given executor service
     * @param timeout timeout to await shutting down
     */
    public static void shutdown(ExecutorService threadPool, final int timeout) {
        try {
            if (!threadPool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            threadPool.shutdownNow();
        }
    }

    /**
     * Returns string representation of buffer content
     *
     * @param buffer given byte buffer
     * @return string representation of byte buffer content
     */
    public static String decodeBytebuffer(final ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }
}
