package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles reply side of the protocol
 */
public class Reply extends Protocol implements Runnable {
    private static final String ERROR_SEND = "Unable to send reply. Server returned with error: ";

    private byte[] request;
    private byte[] uniqueId;

    private int port;

    private AtomicBoolean shutdown;
    private ConcurrentHashMap<BigInteger, byte[]> store;
    private ConcurrentHashMap<BigInteger, byte[]> cache;
    private DatagramPacket requestPacket;
    private DatagramSocket server;
    private final LinkedList<BigInteger> queue;

    private InetAddress ip;

    public Reply(AtomicBoolean shutdown, DatagramSocket server, DatagramPacket requestPacket,
                 ConcurrentHashMap<BigInteger, byte[]> cache, LinkedList<BigInteger> queue,
                 ConcurrentHashMap<BigInteger, byte[]> store) {
        this.shutdown = shutdown;
        this.server = server;
        this.requestPacket = requestPacket;
        this.cache = cache;
        this.queue = queue;
        this.store = store;
    }

    /**
     * Checks the command and act accordingly. Sends a reply packet back to client with appropriate response.
     */
    @Override
    public void run() {
        ip = requestPacket.getAddress();
        port = requestPacket.getPort();

        request = requestPacket.getData();

        uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);
        BigInteger uniqueIdInt = new BigInteger(uniqueId);

        byte[] retry = cache.get(uniqueIdInt);
        if (retry != null) {
            try {
                server.send(new DatagramPacket(retry, retry.length - Long.BYTES, ip, port));
            } catch (IOException e) {
                System.out.println(ERROR_SEND + e.getMessage());
            }

            return;
        }

        DatagramPacket replyPacket;
        byte command = request[LENGTH_UNIQUE_ID];
        switch(command) {
            case COMMAND_PUT:
                try {
                    replyPacket = commandPut();
                } catch (OutOfMemoryError e) {
                    replyPacket = createReplyPacket(ERROR_MEMORY);
                } catch (Exception e) {
                    replyPacket = createReplyPacket(ERROR_FAILURE);
                }
                break;
            case COMMAND_GET:
                replyPacket = commandGet();
                break;
            case COMMAND_REMOVE:
                replyPacket = commandRemove();
                break;
            case COMMAND_SHUTDOWN:
                shutdown.set(true);
                replyPacket = createReplyPacket(ERROR_NONE);
                break;
            case COMMAND_REMOVE_ALL:
                store.clear();
                replyPacket = createReplyPacket(ERROR_NONE);
                break;
            default:
                replyPacket = createReplyPacket(ERROR_COMMAND);
                break;
        }

        if (Runtime.getRuntime().freeMemory() < MAX_MEMORY)
            replyPacket = createReplyPacket(ERROR_MEMORY);

        try {
            server.send(replyPacket);

            ByteUtil.longToByteArray(request, System.currentTimeMillis() + TIMEOUT, request.length - Long.BYTES);
            cache.put(uniqueIdInt, request);
            queue.addLast(uniqueIdInt);

            synchronized (queue) {
                queue.notify();
            }
        } catch (IOException e) {
            System.out.println(ERROR_SEND + e.getMessage());
        }
    }

    /**
     * Get value from store with given key
     */
    private DatagramPacket commandGet() {
        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        BigInteger keyInt = new BigInteger(key);

        try {
            byte[] value = store.get(keyInt);
            if (value == null)
                return createReplyPacket(ERROR_KEY);
            return createReplyPacket(value);
        } catch (Exception e) {
            return createReplyPacket(ERROR_FAILURE);
        }
    }

    /**
     * Put key-value pair into store
     */
    private DatagramPacket commandPut()  {
        if (requestPacket.getLength() <= LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH)
            return createReplyPacket(ERROR_VALUE);

        int valueLength = ByteUtil.leb2int(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY, LENGTH_VALUE_LENGTH);

        if (valueLength < 1 || valueLength > LENGTH_VALUE)
            return createReplyPacket(ERROR_LENGTH);
        else if (requestPacket.getLength() < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + valueLength)
            return createReplyPacket(ERROR_VALUE);
        else if (store.size() == MAX_STORE)
            return createReplyPacket(ERROR_MEMORY);

        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        BigInteger keyInt = new BigInteger(key);

        byte[] value = new byte[valueLength];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH, value, 0, valueLength);

        try {
            store.put(keyInt, value);
            return createReplyPacket(ERROR_NONE);
        } catch (OutOfMemoryError e) {
            return createReplyPacket(ERROR_MEMORY);
        } catch (Exception e) {
            return createReplyPacket(ERROR_FAILURE);
        }
    }

    /**
     * Remove key-value pair from store
     */
    private DatagramPacket commandRemove() {
        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        BigInteger keyInt = new BigInteger(key);
        try {
            if (store.remove(keyInt) == null)
                return createReplyPacket(ERROR_KEY);
            else
                return createReplyPacket(ERROR_NONE);
        } catch (Exception e) {
            return createReplyPacket(ERROR_FAILURE);
        }
    }

    /**
     * Creates a reply packet with an error code
     *
     * @param errorCode - code for client's request
     * @return packet to send back to client
     */
    private DatagramPacket createReplyPacket(byte errorCode) {
        request = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + Long.BYTES];

        System.arraycopy(uniqueId, 0, request, 0, LENGTH_UNIQUE_ID);

        request[LENGTH_UNIQUE_ID] = errorCode;

        return new DatagramPacket(request, request.length - Long.BYTES, ip, port);
    }

    /**
     * Creates a reply packet with value from store
     *
     * @param value - value retreived from store
     * @return packet to send back to client
     */
    private DatagramPacket createReplyPacket(byte[] value) {
        request = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH + value.length + Long.BYTES];

        int index = 0;

        System.arraycopy(uniqueId, 0, request, index, LENGTH_UNIQUE_ID);
        index += LENGTH_UNIQUE_ID;

        request[index] = ERROR_NONE;
        index += LENGTH_CODE;

        ByteUtil.int2leb(value.length, request, index, LENGTH_VALUE_LENGTH);
        index += LENGTH_VALUE_LENGTH;

        System.arraycopy(value, 0, request, index, value.length);

        return new DatagramPacket(request, request.length - Long.BYTES, ip, port);
    }
}
