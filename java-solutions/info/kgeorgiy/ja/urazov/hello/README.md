## HelloUPD

This folder contains two tasks:

**HelloUDP**: 

1. Implement a class server class `HelloUDPServer` and a client classs `HelloUDPClient`, which shall communicate via UDP protocol.
2. `HelloUDPClient` should be sending requests to server, accept the answer and print it to console
    - Command line arguments:
        1. the name or ip address of the computer running the server;
        2. the port number to send requests to;
        3. request prefix (string);
        4. number of concurrent request threads;
        5. the number of requests in each thread.
    - Requests must be sent simultaneously in the specified number of threads.
    - Each thread must wait for its request to be processed and output the request itself and the result of its processing to the console. 
    - If the request was not processed, you need to send it again.
    - Requests should be formed according to the `<request prefix>scheme<stream number>_<request number in the stream>`.
3. The `HelloUDPServer` class must accept and respond to tasks sent by the `HelloUDPClient` class.
    - Command line arguments:
        1. the number of the port on which requests will be received;
        2. the number of worker threads that will process requests.
        3. The response to the request should be Hello, < request text>.
        4. If the server does not have time to process requests, the reception of requests may be temporarily suspended.

**HelloNonblockingUDP**:

1. Implement client and server communication over UDP using only non-blocking I / O.
2. The `HelloUDPNonblockingClient` class should have functionality similar to HelloUDPClient, but without creating new threads.
3. The `HelloUDPNonblockingServer` class must have functionality similar to HelloUDPServer, but all socket operations must be performed in a single thread.
4. The implementation should not have active expectations, including through the Selector.
5. Pay attention to highlighting the common code of the old and new implementation.
6. _Bonus option (not implemented)_. The client and server can allocate O(number of threads) of memory before starting work. It is forbidden to allocate additional memory during operation.
