package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteOrder;

import java.io.IOException;
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

    private static final String ERROR_SEND = "Unable to send reply. Server returned with error: ";

    private byte[] request;
    private byte[] uniqueId;

    private int port;

    private AtomicBoolean shutdown;
    private ConcurrentHashMap<byte[], byte[]> store;
    private DatagramPacket requestPacket;
    private DatagramSocket server;

    private InetAddress ip;

    public Reply(AtomicBoolean shutdown, DatagramSocket server, DatagramPacket requestPacket, ConcurrentHashMap<byte[], byte[]> store) {
        this.shutdown = shutdown;
        this.server = server;
        this.requestPacket = requestPacket;
        this.store = store;
    }

    @Override
    public void run() {
        ip = requestPacket.getAddress();
        port = requestPacket.getPort();

        request = requestPacket.getData();

        uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);

        DatagramPacket replyPacket;
        byte command = request[LENGTH_UNIQUE_ID];
        switch(command) {
            case COMMAND_PUT:
                replyPacket = commandPut();
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

        try {
            server.send(replyPacket);
        } catch (IOException e) {
            System.out.println(ERROR_SEND + e.getMessage());
        }
    }

    private DatagramPacket commandGet() {
        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);

        try {
            byte[] value = store.get(key);
            if (value == null) {
                return createReplyPacket(ERROR_KEY);
            } else {
                return createReplyPacket(value);
            }
        } catch (Exception e) {
            return createReplyPacket(ERROR_FAILURE);
        }
    }

    private DatagramPacket commandPut()  {
        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        int valueLength = ByteOrder.leb2int(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY, LENGTH_VALUE_LENGTH);

        if (valueLength < 1 || valueLength > 10000)
            return createReplyPacket(ERROR_LENGTH);

        byte[] value = new byte[valueLength];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH, value, 0, valueLength);

        try {
            store.put(key, value);
            return createReplyPacket(ERROR_NONE);
        } catch (OutOfMemoryError e) {
            return createReplyPacket(ERROR_MEMORY);
        } catch (Exception e) {
            return createReplyPacket(ERROR_FAILURE);
        }
    }

    private DatagramPacket commandRemove() {
        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);

        try {
            if (store.remove(key) == null)
                return createReplyPacket(ERROR_KEY);
            else
                return createReplyPacket(ERROR_NONE);
        } catch (Exception e) {
            return createReplyPacket(ERROR_FAILURE);
        }
    }

    private DatagramPacket createReplyPacket(byte errorCode) {
        byte[] reply = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE];

        System.arraycopy(uniqueId, 0, reply, 0, LENGTH_UNIQUE_ID);

        reply[LENGTH_UNIQUE_ID] = errorCode;

        return new DatagramPacket(reply, reply.length, ip, port);
    }

    private DatagramPacket createReplyPacket(byte[] value) {
        byte[] reply = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH + value.length];

        int index = 0;

        System.arraycopy(uniqueId, 0, reply, index, LENGTH_UNIQUE_ID);
        index += LENGTH_UNIQUE_ID;

        reply[index] = ERROR_NONE;
        index += LENGTH_CODE;

        byte[] valueLength = new byte[LENGTH_VALUE_LENGTH];
        ByteOrder.int2leb(value.length, valueLength, index);
        index += LENGTH_VALUE_LENGTH;

        System.arraycopy(value, 0, reply, index, value.length);

        return new DatagramPacket(reply, reply.length, ip, port);
    }
}
