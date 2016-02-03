package com.s26643114.CPEN431.protocol;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles client's retries
 */
public class Retry extends Protocol implements Runnable {
    private BigInteger uniqueId;
    private ConcurrentHashMap<BigInteger, DatagramPacket> queue;

    public Retry(BigInteger uniqueId, ConcurrentHashMap<BigInteger, DatagramPacket> queue) {
        this.uniqueId = uniqueId;
        this.queue = queue;
    }

    /**
     * Waits to see if client wants to retry and resends reply. Removes itself from queue when timeout or retries run out.
     */
    @Override
    public void run() {
        try {
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(TIMEOUT);
            }
        } catch (InterruptedException ignored) {}

        queue.remove(uniqueId);
    }
}
