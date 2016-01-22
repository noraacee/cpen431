package com.s26643114.CPEN431.protocol;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles client's retries
 */
public class Retry extends Protocol implements Runnable {
    private static final int TIMEOUT_FLEX = 50; //adds a small delay to timeout to account for server latency

    private static final String ERROR_SEND = "Unable to send reply. Server returned with error: ";

    private int retries;

    private long timeout;

    private BigInteger uniqueId;
    private ConcurrentHashMap<BigInteger, Thread> queue;
    private DatagramPacket replyPacket;
    private DatagramSocket server;

    public Retry(DatagramSocket server, DatagramPacket replyPacket, BigInteger uniqueId, ConcurrentHashMap<BigInteger, Thread> queue) {
        this.server = server;
        this.replyPacket = replyPacket;
        this.uniqueId = uniqueId;
        this.queue = queue;

        retries = RETRIES;
        timeout = TIMEOUT;
    }

    /**
     * Waits to see if client wants to retry and resends reply. Removes itself from queue when timeout or retries run out.
     */
    @Override
    public void run() {
        while(retries > 0) {
            try {
                synchronized (Thread.currentThread()) {
                    Thread.currentThread().wait(timeout + TIMEOUT_FLEX);
                }
            } catch (InterruptedException retry) {
                try {
                    System.out.println("Hur");
                    server.send(replyPacket);
                } catch (IOException e) {
                    System.out.println(ERROR_SEND + e.getMessage());
                }
            }

            timeout *= 2;
            retries--;
        }

        queue.remove(uniqueId);
    }
}
