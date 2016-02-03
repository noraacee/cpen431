package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteOrder;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles reply side of the protocol
 */
public class Reply extends Protocol implements Runnable {
    private static final byte COMMAND_PUT = 0x01;
    private static final byte COMMAND_GET = 0x02;
    private static final byte COMMAND_REMOVE = 0x03;
    private static final byte COMMAND_SHUTDOWN = 0x04;
    private static final byte COMMAND_REMOVE_ALL = 0x05;

    private static final byte ERROR_NONE = 0x00; //operation is successful
    private static final byte ERROR_KEY = 0x01; //non-existent key requested in a get or delete operation
    private static final byte ERROR_MEMORY = 0x02; //out of memory for put operation
    private static final byte ERROR_OVERLOAD = 0x03; //system overload
    private static final byte ERROR_FAILURE = 0x04; //internal failure
    private static final byte ERROR_COMMAND = 0x05; //unrecognized command
    private static final byte ERROR_LENGTH = 0x06; // value length is invalid (cannot be less than 1 or more than 10000
    private static final byte ERROR_VALUE = 0x07; // invalid value field

    private static final String ERROR_SEND = "Unable to send reply. Server returned with error: ";

    private byte[] request;
    private byte[] uniqueId;

    private int port;

    private AtomicBoolean shutdown;
    private ConcurrentHashMap<BigInteger, byte[]> store;
    private ConcurrentHashMap<BigInteger, DatagramPacket> queue;
    private DatagramPacket requestPacket;
    private DatagramSocket server;

    private InetAddress ip;

    public Reply(AtomicBoolean shutdown, DatagramSocket server, DatagramPacket requestPacket, ConcurrentHashMap<BigInteger, DatagramPacket> queue, ConcurrentHashMap<BigInteger, byte[]> store) {
        this.shutdown = shutdown;
        this.server = server;
        this.requestPacket = requestPacket;
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

        DatagramPacket retry = queue.get(uniqueIdInt);
        if (retry != null) {
            try {
                server.send(retry);
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

            //starts retry thread with all information needed to resend a reply
            Thread retryThread = new Thread(new Retry(uniqueIdInt, queue));
            queue.put(uniqueIdInt, replyPacket);
            retryThread.start();
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
            else
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

        int valueLength = ByteOrder.leb2int(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY, LENGTH_VALUE_LENGTH);

        if (valueLength < 1 || valueLength > LENGTH_VALUE)
            return createReplyPacket(ERROR_LENGTH);
        else if (requestPacket.getLength() < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + valueLength)
            return createReplyPacket(ERROR_VALUE);
        else if (store.size() == MAX_STORE)
            return createReplyPacket(ERROR_MEMORY);
        else if (Runtime.getRuntime().freeMemory() < MAX_MEMORY)
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
        byte[] reply = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE];

        System.arraycopy(uniqueId, 0, reply, 0, LENGTH_UNIQUE_ID);

        reply[LENGTH_UNIQUE_ID] = errorCode;

        return new DatagramPacket(reply, reply.length, ip, port);
    }

    /**
     * Creates a reply packet with value from store
     *
     * @param value - value retreived from store
     * @return packet to send back to client
     */
    private DatagramPacket createReplyPacket(byte[] value) {
        byte[] reply = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH + value.length];

        int index = 0;

        System.arraycopy(uniqueId, 0, reply, index, LENGTH_UNIQUE_ID);
        index += LENGTH_UNIQUE_ID;

        reply[index] = ERROR_NONE;
        index += LENGTH_CODE;

        byte[] valueLength = new byte[Integer.BYTES];
        ByteOrder.int2leb(value.length, valueLength, 0);
        System.arraycopy(valueLength, 0, reply, index, LENGTH_VALUE_LENGTH);
        index += LENGTH_VALUE_LENGTH;

        System.arraycopy(value, 0, reply, index, value.length);

        return new DatagramPacket(reply, reply.length, ip, port);
    }
}
