package info.kgeorgiy.ja.urazov.hello;

import java.nio.ByteBuffer;
import java.util.List;

public class HelloUDPClientChannelData {
    private ByteBuffer content;
    private final int numOfThread;
    private final int requestNum;
    private int currentRequest;

    /**
     * Creates an instance of {@code HelloUDPClientChannelData} which contains information
     * about clients channel (number of thread, current request and buffer with message)
     *
     * @param numOfThread number of thread
     * @param requests total amount of requests
     * @param bufferSize size of buffer with message
     */
    public HelloUDPClientChannelData(final int numOfThread,
                                     final int requests,
                                     final int bufferSize) {
        this.numOfThread = numOfThread;
        requestNum = requests;
        content = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Increments number of request
     *
     */
    public void incrementRequest() {
        currentRequest++;
    }

    /**
     * Checks whether all requests were sent
     *
     * @return status indicating whether all requests were sent
     */
    public boolean limitAttained() {
        return currentRequest == requestNum;
    }

    /**
     * Returns number of thread and next request
     *
     * @return number of thread and next request
     */
    public List<Integer> nextRequestNum() {
        return List.of(numOfThread, currentRequest);
    }

    /**
     * Retrieves byte buffer
     *
     * @return byte buffer
     */
    public ByteBuffer getContent() {
        return content;
    }

    /**
     * Sets byte buffer
     *
     * @param content byte buffer
     */
    public void setContent(ByteBuffer content) {
        this.content = content;
    }
}
