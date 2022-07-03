package info.kgeorgiy.ja.urazov.hello;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Record containing byte buffer and socket address which are ready to send
 *
 * @param socketAddress socket address
 * @param buffer byte buffer
 */
public record ResponseData(SocketAddress socketAddress, ByteBuffer buffer) {}
