package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Database extends Protocol {
    private ConcurrentHashMap<BigInteger, byte[]> cache;
    private ConcurrentHashMap<BigInteger, byte[]> database;
    private ConcurrentHashMap<Long, AtomicBoolean> internalRequests;
    private ConcurrentLinkedQueue<Client> pool;
    private final ConcurrentLinkedQueue<BigInteger> queue;

    private int poolSize;

    public BigInteger queueFirst;

    public Database() {
        cache = new ConcurrentHashMap<>();
        database = new ConcurrentHashMap<>();
        internalRequests = new ConcurrentHashMap<>();
        pool = new ConcurrentLinkedQueue<>();
        queue = new ConcurrentLinkedQueue<>();

        poolSize = 0;
    }

    /**
     * Caches a reply
     */
    public void cache(BigInteger uniqueId, byte[] reply) {
        byte[] value = cache.put(uniqueId, reply);

        if (value == null)
            queue.add(uniqueId);

        synchronized (queue) {
            queue.notify();
        }

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "cache [" + uniqueId + ":" + reply.length + "]");
    }

    /**
     * Checks if a reply is already in the cache
     */
    public byte[] check(BigInteger uniqueId) {
        byte[] reply = cache.get(uniqueId);

        if (Logger.VERBOSE_DATABASE) {
            if (reply == null)
                Logger.log(Logger.TAG_DATABASE, "cache not found: " + uniqueId);
            else
                Logger.log(Logger.TAG_DATABASE, "cache found: [" + uniqueId + ":" + reply.length + " ]");
        }

        return reply;
    }

    /**
     * Clears the entire database
     */
    public void clear() {
        database.clear();

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "clear");
    }


    /**
     * Gets a value from the database
     */
    public byte[] get(BigInteger key) {
        byte[] value = database.get(key);

        if (Logger.VERBOSE_DATABASE) {
            if (value == null)
                Logger.log(Logger.TAG_DATABASE, "get key not found: " + key);
            else
                Logger.log(Logger.TAG_DATABASE, "get: [" + key + ": " + value.length + "]");
        }

        return value;
    }

    /**
     * Gets the timeout until the next reply times out
     */
    public long getTimeout() {
        if (queue.isEmpty()) {
            try {
                synchronized (queue) {
                    if (Logger.VERBOSE_DATABASE)
                        Logger.log(Logger.TAG_DATABASE, "waiting for queue");

                    queue.wait();
                }
            } catch (InterruptedException e) {
                if (Logger.VERBOSE_DATABASE)
                    Logger.log(Logger.TAG_DATABASE, "queue interrupted");
            }
        }

        queueFirst = queue.poll();
        byte[] retry = cache.get(queueFirst);

        long timeout = ByteUtil.byteArrayToLong(retry, retry.length - LENGTH_INSTANT) - System.currentTimeMillis();

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "timeout: " + timeout + " ms");

        return timeout;
    }

    public Client pop(Server server, AtomicBoolean shutdown) {
        if (pool.isEmpty()) {
            if (Logger.VERBOSE_DATABASE)
                Logger.log(Logger.TAG_DATABASE, "client pool empty");

            return new Client(server, shutdown, this);
        } else {
            try {
                Client client = pool.remove();
                if (poolSize > 0)
                    poolSize--;
                client.init();

                if (Logger.VERBOSE_DATABASE)
                    Logger.log(Logger.TAG_DATABASE, "popped client from pool");

                return client;
            } catch (NoSuchElementException e) {
                if (Logger.VERBOSE_DATABASE)
                    Logger.log(Logger.TAG_DATABASE, "client pool empty");

                return new Client(server, shutdown, this);
            }
        }
    }

    public void push(Client client) {
        if (poolSize >= SIZE_POOL_CLIENT) {
            if (Logger.VERBOSE_DATABASE)
                Logger.log(Logger.TAG_DATABASE, "client pool full");

            return;
        }

        pool.add(client);
        poolSize++;

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "pushed client to pool");
    }

    /**
     * Puts a value into database
     */
    public void put(BigInteger key, byte[] value) {
        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "put [" + key + ":" + value.length + "]");

        database.put(key, value);
    }

    /**
     * Put thread lock into lock store
     */
    public AtomicBoolean put(long key, AtomicBoolean lock) {
        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "put internal: " + key);

        return internalRequests.put(key, lock);
    }

    /**
     * Returns the number of values in the database
     */
    public int size() {
        int size = database.size();

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "database size: " + size);

        return size;
    }


    /**
     * Removes a value from the database
     */
    public byte[] remove(BigInteger key) {
        byte[] removed = database.remove(key);

        if (Logger.VERBOSE_DATABASE) {
            if (removed == null)
                Logger.log(Logger.TAG_DATABASE, "remove key not found: " + key);
            else
                Logger.log(Logger.TAG_DATABASE, "remove [" + key + ":" + removed.length + "]");
        }

        return removed;
    }

    /**
     * Removes and returns a thread lock from the lock store
     */
    public AtomicBoolean remove(long key) {
        AtomicBoolean lock = internalRequests.remove(key);

        if (Logger.VERBOSE_DATABASE) {
            if (lock == null)
                Logger.log(Logger.TAG_DATABASE, "remove internal key not found: " + key);
            else
                Logger.log(Logger.TAG_DATABASE, "remove internal: " + key);
        }

        return lock;
    }

    /**
     * Removes a reply from the cache
     */
    public void removeCache() {
        byte[] removed = cache.remove(queueFirst);

        if (Logger.VERBOSE_DATABASE) {
            if (removed == null)
                Logger.log(Logger.TAG_DATABASE, "cached reply not found: " + queueFirst);
            else
                Logger.log(Logger.TAG_DATABASE, "remove cache [" + queueFirst + ":" + removed.length + "]");
        }
    }
}
