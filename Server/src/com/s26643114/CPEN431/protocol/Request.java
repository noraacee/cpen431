package com.s26643114.CPEN431.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles request side of the protocol
 */
public class Request extends Protocol {
    private static final String ERROR_SEND = "Unable to receive reply. Server returned with error: ";

    private AtomicBoolean shutdown;
    private ConcurrentHashMap<byte[], byte[]> store;
    private DatagramSocket server;

    public Request(DatagramSocket server, ConcurrentHashMap<byte[], byte[]> store) {
        this.server = server;
        this.store = store;

        shutdown = new AtomicBoolean(false);
    }

    public void receive() {
        while(!shutdown.get()) {
            byte[] request = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + LENGTH_VALUE];
            DatagramPacket requestPacket = new DatagramPacket(request, request.length);

            try {
                server.receive(requestPacket);

                new Thread(new Reply(shutdown, server, requestPacket, store)).start();
            } catch (IOException e) {
                System.out.println(ERROR_SEND + e.getMessage());
            }
        }
    }
}
