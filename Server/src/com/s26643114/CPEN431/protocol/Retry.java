package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteUtil;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles client's retries
 */
public class Retry extends Protocol implements Runnable {
    private AtomicBoolean shutdown;
    private ConcurrentHashMap<BigInteger, byte[]> cache;
    private final LinkedList<BigInteger> queue;

    public Retry(AtomicBoolean shutdown, ConcurrentHashMap<BigInteger, byte[]> cache, LinkedList<BigInteger> queue) {
        this.shutdown = shutdown;
        this.cache = cache;
        this.queue = queue;
    }

    /**
     * Waits to see if client wants to retry and resends reply. Removes itself from timeCache when timeout or retries run out.
     */
    @Override
    public void run() {
        BigInteger key;
        byte[] retry;
        long timeout;
        long instant;

        while(!shutdown.get()) {
            if (queue.isEmpty()) {
                synchronized (queue) {
                    try {
                        queue.wait();
                    } catch (InterruptedException ignored) {}
                }
            } else {
                key = queue.getFirst();
                retry = cache.get(key);
                timeout = ByteUtil.byteArrayToLong(retry, retry.length - Long.BYTES);
                instant = System.currentTimeMillis();

                if (timeout > instant) {
                    synchronized (this) {
                        try {
                            wait(timeout - instant);
                        } catch (InterruptedException ignored) {}
                    }
                }

                cache.remove(key);
                queue.removeFirst();
            }
        }
    }
}
