package com.s26643114.CPEN431.protocol;

import java.math.BigInteger;
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
    private ConcurrentHashMap<BigInteger, byte[]> store; //key-value store
    private ConcurrentHashMap<BigInteger, DatagramPacket> queue; //queue to cache completed requests for retrying
    private DatagramSocket server;

    public Request(DatagramSocket server, ConcurrentHashMap<BigInteger, byte[]> store) {
        this.server = server;
        this.store = store;

        shutdown = new AtomicBoolean(false);
        queue = new ConcurrentHashMap<>();
    }

    /**
     * Server constantly receives from socket and starts new thread for each request
     */
    public void receive() {
        while(!shutdown.get()) {
            try {
                byte[] request = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + LENGTH_VALUE];
                DatagramPacket requestPacket = new DatagramPacket(request, request.length);
                server.receive(requestPacket);

                new Thread(new Reply(shutdown, server, requestPacket, queue, store)).start();
            } catch (Exception e) {
                System.out.println(ERROR_SEND + e.getMessage());
            }
        }
    }
}
