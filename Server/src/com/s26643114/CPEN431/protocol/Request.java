package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.system.Database;
import com.s26643114.CPEN431.util.ByteUtil;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates a request and its reply
 */
public class Request extends Protocol {
    private byte[] request;
    private byte[] reply;
    private byte[] uniqueId;

    private AtomicBoolean shutdown;
    private BigInteger uniqueIdInt;
    private Database database;
    private DatagramPacket packet;

    public Request(AtomicBoolean shutdown, Database database, DatagramPacket packet) {
        this.shutdown = shutdown;
        this.database = database;
        this.packet = packet;

        request = packet.getData();

        parseUniqueId();
    }

    /**
     * Cache the request in the database
     */
    public void cache() {
        ByteUtil.longToByteArray(reply, System.currentTimeMillis() + TIMEOUT, reply.length - Long.BYTES);
        database.cache(uniqueIdInt, reply);
    }

    /**
     * Parses and completes the request
     */
    public DatagramPacket parse() {
        //Checks if request is in cache
        byte[] retry = database.check(uniqueIdInt);
        if (retry != null) {
            Reply.setReply(packet, retry);
            return packet;
        }

        byte command = request[LENGTH_UNIQUE_ID];
        switch (command) {
            case COMMAND_PUT:
                reply = put();
                break;
            case COMMAND_GET:
                reply = get();
                break;
            case COMMAND_REMOVE:
                reply = remove();
                break;
            case COMMAND_SHUTDOWN:
                shutdown.set(true);
                reply = Reply.createReply(packet, uniqueId, ERROR_NONE);
                break;
            case COMMAND_REMOVE_ALL:
                database.clear();
                reply = Reply.createReply(packet, uniqueId, ERROR_NONE);
                break;
            default:
                reply = Reply.createReply(packet, uniqueId, ERROR_COMMAND);
                break;
        }

        return packet;
    }

    /**
     * Rejects the request with the error code
     */
    public DatagramPacket reject(byte errorCode) {
        Reply.setReply(packet, errorCode);
        return packet;
    }

    /**
     * Get value from store with given key
     */
    private byte[] get() {
        byte[] value = database.get(parseKey());
        if (value == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, value);
    }

    /**
     * Parses the key to use in the command
     */
    private BigInteger parseKey() {
        byte[] key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        return new BigInteger(key);
    }

    /**
     * Parses the unique id of the request
     */
    private void parseUniqueId() {
        uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);
        uniqueIdInt = new BigInteger(uniqueId);
    }

    /**
     * Put key-value pair into store
     */
    private byte[] put()  {
        // Checks if packet contains a value to put
        if (packet.getLength() <= LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH)
            return Reply.createReply(packet, uniqueId, ERROR_VALUE);

        int valueLength = ByteUtil.leb2int(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY, LENGTH_VALUE_LENGTH);

        // Checks if the length of the value to put is valid
        if (valueLength < 1 || valueLength > LENGTH_VALUE)
            return Reply.createReply(packet, uniqueId, ERROR_LENGTH);

        // Checks if the packet length is valid with the length of the value
        else if (packet.getLength() < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + valueLength)
            return Reply.createReply(packet, uniqueId, ERROR_VALUE);

        // Checks if there is room in the database
        else if (database.size() >= MAX_STORE)
            return Reply.createReply(packet, uniqueId, ERROR_MEMORY);

        BigInteger key = parseKey();
        byte[] value = new byte[valueLength];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH, value, 0, valueLength);

        database.put(key, value);
        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }

    /**
     * Remove key-value pair from store
     */
    private byte[] remove() {
        if (database.remove(parseKey()) == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        else
            return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }
}
