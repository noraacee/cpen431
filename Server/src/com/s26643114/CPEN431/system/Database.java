package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.util.ByteUtil;
//import com.s26643114.CPEN431.util.Logger;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Database extends Protocol {
    //private static final String TAG = "database";

    private ConcurrentHashMap<BigInteger, byte[]> cache;
    private ConcurrentHashMap<BigInteger, byte[]> database;
    private ConcurrentHashMap<Long, AtomicBoolean> internalRequests;
    private final ConcurrentLinkedQueue<BigInteger> queue;

    public BigInteger queueFirst;

    public Database() {
        cache = new ConcurrentHashMap<>();
        database = new ConcurrentHashMap<>();
        internalRequests = new ConcurrentHashMap<>();
        queue = new ConcurrentLinkedQueue<>();
    }

    public void cache(BigInteger uniqueId, byte[] reply) {
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "caching reply [" + uniqueId + ":" + reply.length + "]");

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
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "getting key: " + key);
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

        return ByteUtil.byteArrayToLong(retry, retry.length - LENGTH_INSTANT) - System.currentTimeMillis();
    }

    public void put(BigInteger key, byte[] value) {
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "putting [" + key + ":" + value.length + "]");

        database.put(key, value);
    }

    public AtomicBoolean put(long key, AtomicBoolean lock) {
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "putting internal key: " + key);

        return internalRequests.put(key, lock);
    }

    public int size() {
        return database.size();
    }

    public byte[] remove(BigInteger key) {
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "removing key: " + key);
        return database.remove(key);
    }

    public AtomicBoolean remove(long key) {
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "removing internal key: " + key);

        return internalRequests.remove(key);
    }

    public void removeCache() {
        //if (Logger.VERBOSE_DATABASE)
            //Logger.log(TAG, "removing cache key: " + queueFirst);

        cache.remove(queueFirst);
    }
}
