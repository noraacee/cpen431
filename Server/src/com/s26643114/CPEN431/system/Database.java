package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Database {
    private ConcurrentHashMap<BigInteger, byte[]> cache;
    private ConcurrentHashMap<BigInteger, byte[]> database;
    private ConcurrentLinkedQueue<Client> pool;
    private final ConcurrentLinkedQueue<BigInteger> queue;

    private int poolSize;

    public BigInteger queueFirst;

    public Database() {
        cache = new ConcurrentHashMap<>();
        database = new ConcurrentHashMap<>();
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
            queue.offer(uniqueId);

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

        long timeout = ByteUtil.byteArrayToLong(retry, retry.length - Protocol.LENGTH_INSTANT) - System.currentTimeMillis();

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "timeout: " + timeout + " ms");

        return timeout;
    }

    /**
     * Pops an unused client from the client pool or creates a new client if none is available
     */
    public Client poll(Server server) {
        if (pool.isEmpty()) {
            if (Logger.VERBOSE_DATABASE)
                Logger.log(Logger.TAG_DATABASE, "client pool empty");

            return new Client(server, this);
        } else {
            try {
                Client client = pool.poll();
                if (poolSize > 0)
                    poolSize--;
                client.reset();

                if (Logger.VERBOSE_DATABASE)
                    Logger.log(Logger.TAG_DATABASE, "popped client from pool");

                return client;
            } catch (NoSuchElementException e) {
                if (Logger.VERBOSE_DATABASE)
                    Logger.log(Logger.TAG_DATABASE, "client pool empty");

                return new Client(server, this);
            }
        }
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
     * Pushes a finished client to the client pool as long as it does not exceed client pool's max size
     */
    public void offer(Client client) {
        if (poolSize >= Protocol.SIZE_POOL_CLIENT) {
            if (Logger.VERBOSE_DATABASE)
                Logger.log(Logger.TAG_DATABASE, "client pool full");

            return;
        }

        pool.offer(client);
        poolSize++;

        if (Logger.VERBOSE_DATABASE)
            Logger.log(Logger.TAG_DATABASE, "pushed client to pool");
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
