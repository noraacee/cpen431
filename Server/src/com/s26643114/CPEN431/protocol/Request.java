package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.distribution.Distribution;
import com.s26643114.CPEN431.system.Database;
import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates a request and its reply
 */
public class Request extends Protocol {
    private byte[] key;
    private byte[] request;
    private byte[] reply;
    private byte[] uniqueId;

    private AtomicBoolean shutdown;
    private BigInteger keyInt;
    private BigInteger uniqueIdInt;
    private Database database;
    private DatagramPacket packet;

    public Request(AtomicBoolean shutdown, Database database, DatagramPacket packet) {
        this.shutdown = shutdown;
        this.database = database;
        this.packet = packet;

        request = packet.getData();
        reply = null;

        parseKey();
        parseUniqueId();
    }

    /**
     * Cache the request in the database
     */
    public void cache() {
        if (reply == null)
            return;

        ByteUtil.longToByteArray(reply, System.currentTimeMillis() + TIMEOUT, reply.length - Long.BYTES);
        database.cache(uniqueIdInt, reply);
    }

    public DatagramPacket convertToInternal(InetAddress ip, int port) {
        if (request[LENGTH_UNIQUE_ID] < MASK_COMMAND)
            request[LENGTH_UNIQUE_ID] += MASK_COMMAND;

        System.arraycopy(packet.getAddress().getAddress(), 0, request, packet.getLength(), LENGTH_IP);

        byte[] portByte = new byte[LENGTH_PORT];
        ByteUtil.int2leb(packet.getPort(), portByte, 0, LENGTH_PORT);
        System.arraycopy(portByte, 0, request, packet.getLength() + LENGTH_IP, LENGTH_PORT);

        packet.setLength(packet.getLength() + LENGTH_IP + LENGTH_PORT);
        packet.setAddress(ip);
        packet.setPort(port);

        if (Logger.VERBOSE_REQUEST)
            Logger.log("packet converted to command " + request[LENGTH_UNIQUE_ID] + ", ip " + ip.getHostAddress()
                    + ", port " + port + " and packet length " + packet.getLength());

        return packet;
    }

    public byte[] getKey() {
        return key;
    }

    /**
     * Parses and completes the request
     */
    public DatagramPacket parse() throws IOException {
        if (uniqueIdInt == null) {
            Reply.setReply(packet, ERROR_PACKET);
            return packet;
        }

        //Checks if request is in cache
        byte[] retry = database.check(uniqueIdInt);
        if (retry != null) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log("reply found in cache with unique id: ");

            Reply.setReply(packet, retry);
            return packet;
        }

        byte command = request[LENGTH_UNIQUE_ID];
        if (command <= COMMAND_REMOVE && !Distribution.checkSelf(key)) {
            Distribution.route(this);
            return null;
        } else {
            if (command > MASK_COMMAND) {
                configureRouting();
                command -= MASK_COMMAND;
            }

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
                    if (Logger.VERBOSE_REQUEST)
                        Logger.log("shutdown requested");

                    shutdown.set(true);
                    reply = Reply.createReply(packet, uniqueId, ERROR_NONE);
                    break;
                case COMMAND_REMOVE_ALL:
                    if (Logger.VERBOSE_REQUEST)
                        Logger.log("remove all request");

                    database.clear();
                    reply = Reply.createReply(packet, uniqueId, ERROR_NONE);
                    break;
                default:
                    if (Logger.VERBOSE_REQUEST)
                        Logger.log("command not found: " + command);

                    reply = Reply.createReply(packet, uniqueId, ERROR_COMMAND);
                    break;
            }

            return packet;
        }
    }

    /**
     * Rejects the request with the error code
     */
    public DatagramPacket reject(byte errorCode) throws UnknownHostException {
        if (request[LENGTH_UNIQUE_ID] > MASK_COMMAND)
            configureRouting();

        if (Logger.VERBOSE_REQUEST)
            Logger.log("rejecting package with code " + errorCode);

        Reply.setReply(packet, errorCode);
        return packet;
    }

    private void configureRouting() throws UnknownHostException {
        byte[] ip = new byte[LENGTH_IP];
        System.arraycopy(request, packet.getLength() - LENGTH_PORT - LENGTH_IP, ip, 0, LENGTH_IP);

        int port = ByteUtil.leb2int(request, packet.getLength() - LENGTH_PORT, LENGTH_PORT);

        packet.setAddress(InetAddress.getByAddress(ip));
        packet.setPort(port);

        if (Logger.VERBOSE_REQUEST)
            Logger.log("configured routing to [" + packet.getAddress().getHostAddress() + ":" + port + "]");
    }

    /**
     * Get value from store with given key
     */
    private byte[] get() {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        byte[] value = database.get(keyInt);

        if (Logger.VERBOSE_REQUEST) {
            if (value == null)
                Logger.log("get key not fount: " + keyInt);
            else
                Logger.log("get [" + keyInt + ": " + value.length + "]");
        }

        if (value == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, value);
    }

    /**
     * Parses the key to use in the command
     */
    private void parseKey() {
        if (packet.getLength() < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY) {
            if (Logger.VERBOSE_REQUEST)
                System.out.println("no key to parse");

            key = null;
            keyInt = null;
            return;
        }

        key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        keyInt = new BigInteger(key);

        if (Logger.VERBOSE_REQUEST)
            Logger.log("key parsed: " + keyInt);
    }

    /**
     * Parses the unique id of the request
     */
    private void parseUniqueId() {
        if (packet.getLength() < LENGTH_UNIQUE_ID + LENGTH_CODE) {
            if (Logger.VERBOSE_REQUEST)
                System.out.println("no unique id to parse");

            uniqueId = null;
            uniqueIdInt = null;
            return;
        }

        uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);
        uniqueIdInt = new BigInteger(uniqueId);

        if (Logger.VERBOSE_REQUEST)
            Logger.log("unique id parsed: " + uniqueIdInt);
    }

    /**
     * Put key-value pair into store
     */
    private byte[] put()  {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

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

        byte[] value = new byte[valueLength];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH, value, 0, valueLength);

        database.put(keyInt, value);

        if (Logger.VERBOSE_REQUEST)
            Logger.log("put [" + keyInt + ":" + valueLength + "]");

        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }

    /**
     * Remove key-value pair from store
     */
    private byte[] remove() {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        byte[] removed = database.remove(keyInt);

        if (Logger.VERBOSE_REQUEST) {
            if (removed == null)
                Logger.log("remove key not found: " + keyInt);
            else
                Logger.log("remove [" + keyInt + ":" + removed.length + "]");
        }

        if (removed == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }
}
