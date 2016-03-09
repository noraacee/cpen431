package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.util.ByteUtil;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Database {
    private ConcurrentHashMap<BigInteger, byte[]> cache;
    private ConcurrentHashMap<BigInteger, byte[]> database;
    private final ConcurrentLinkedQueue<BigInteger> queue;

    public BigInteger queueFirst;

    public Database() {
        cache = new ConcurrentHashMap<>();
        database = new ConcurrentHashMap<>();
        queue = new ConcurrentLinkedQueue<>();
    }

    public void cache(BigInteger uniqueId, byte[] reply) {
        byte[] value = cache.put(uniqueId, reply);

        if (value == null)
            queue.add(uniqueId);

        synchronized (queue) {
            queue.notify();
        }
    }

    public byte[] check(BigInteger uniqueId) {
        return cache.get(uniqueId);
    }

    public void clear() {
        database.clear();
    }

    public byte[] get(BigInteger key) {
        return database.get(key);
    }

    public long getTimeout() {
        if (queue.isEmpty()) {
            try {
                synchronized (queue) {
                    queue.wait();
                }
            } catch (InterruptedException ignored) {
            }
        }

        queueFirst = queue.poll();
        byte[] retry = cache.get(queueFirst);

        return ByteUtil.byteArrayToLong(retry, retry.length - Long.BYTES) - System.currentTimeMillis();
    }

    public void put(BigInteger key, byte[] value) {
        database.put(key, value);
    }

    public int size() {
        return database.size();
    }

    public byte[] remove(BigInteger key) {
        return database.remove(key);
    }

    public void removeFirst() {
        cache.remove(queueFirst);
    }

}
