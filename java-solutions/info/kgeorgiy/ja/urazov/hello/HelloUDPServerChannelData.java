package info.kgeorgiy.ja.urazov.hello;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public final class HelloUDPServerChannelData {
    private int size;
    private final int capacity;
    private final Queue<ResponseData> responses;

    /**
     * Creates an instance of {@code HelloUDPServerChannelData} which contains
     * buffers with responses ready to send back to attached address and handle
     * load by capacity
     *
     * @param capacity capacity of queue of ready responses
     */
    public HelloUDPServerChannelData(final int capacity) {
        this.capacity = capacity;
        responses = new LinkedBlockingDeque<>(capacity);
    }

    /**
     * Adds a response in the form of address and byte buffer
     *
     * @param socketAddress address to send response
     * @param buffer byte buffer
     */
    public void addResponse(final SocketAddress socketAddress, final ByteBuffer buffer) {
        responses.add(new ResponseData(socketAddress, buffer));
    }

    /**
     * Retrieves ready response in the form of address and byte buffer from the responses container
     *
     * @return address and byte buffer
     */
    public ResponseData getResponse() {
        size--;
        return responses.poll();
    }

    /**
     * Reserve space for data in responses container
     *
     */
    public void reserve() {
        size++;
    }

    /**
     * Checks whether no free space to add a response
     *
     * @return status indicating whether there is free space in the queue
     */
    public boolean noDataRemaining() {
        return capacity == size;
    }

    /**
     * Checks whether there is no ready responses
     *
     * @return status indicating whether there is no ready responses
     */
    public boolean noData() {
        return responses.isEmpty();
    }
}
