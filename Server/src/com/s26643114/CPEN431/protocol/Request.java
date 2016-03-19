package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.distribution.Route;
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
    protected Database database;

    private byte[] key;
    private byte[] request;
    private byte[] reply;
    private byte[] uniqueId;

    private int requestLength;

    private AtomicBoolean shutdown;
    private BigInteger keyInt;
    private BigInteger uniqueIdInt;
    private DatagramPacket packet;

    public Request(AtomicBoolean shutdown, Database database) {
        this.shutdown = shutdown;
        this.database = database;

        request = new byte[LENGTH_TOTAL];
        packet = new DatagramPacket(request, LENGTH_TOTAL);
        reply = null;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public void init() {
        packet.setData(request);
        reply = null;
    }

    /**
     * Rejects the request with the error code
     */
    protected DatagramPacket createReject(byte errorCode) throws UnknownHostException {
        if (request[LENGTH_UNIQUE_ID] > MASK_COMMAND)
            configureRouting();

        Reply.setReply(packet, errorCode);

        return packet;
    }

    /**
     * Parses and completes the request
     */
    protected DatagramPacket parse() throws IOException {
        requestLength = packet.getLength();
        parseKey();
        parseUniqueId();

        if (uniqueIdInt == null) {
            Reply.setReply(packet, ERROR_PACKET);
            return packet;
        }

        reply = database.check(uniqueIdInt);
        if (reply != null) {
            if (request[LENGTH_UNIQUE_ID] > MASK_COMMAND)
                configureRouting();

            Reply.setReply(packet, reply);
            return packet;
        }

        byte command = request[LENGTH_UNIQUE_ID];
        if (command <= COMMAND_REMOVE && !Route.checkSelf(key)) {
            convertToInternal();

            long start;
            if (Logger.BENCHMARK_REQUEST)
                start = System.nanoTime();

            Route.route(packet, key);

            if (Logger.BENCHMARK_REQUEST) {
                long end = System.nanoTime();
                Logger.benchmark(Logger.TAG_REQUEST, start, end, "route");
            }

            return null;
        } else {
            if (command > MASK_COMMAND && command <= MASK_COMMAND + COMMAND_REMOVE) {
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
                        Logger.log(Logger.TAG_REQUEST, "shut down requested");

                    shutdown.set(true);
                    Reply.setReply(packet, ERROR_NONE);
                    return packet;
                case COMMAND_REMOVE_ALL:
                    database.clear();
                    reply = Reply.createReply(packet, uniqueId, ERROR_NONE);
                    break;
                default:
                    if (Logger.VERBOSE_REQUEST)
                        Logger.log(Logger.TAG_REQUEST, "command " + command + " not found: " + uniqueIdInt);

                    reply = Reply.createReply(packet, uniqueId, ERROR_COMMAND);
                    break;
            }

            cache();
            return packet;
        }
    }

    /**
     * Cache the request in the database
     */
    private void cache() {
        if (reply == null)
            return;

        ByteUtil.longToByteArray(reply, System.currentTimeMillis() + TIMEOUT, reply.length - LENGTH_INSTANT);
        database.cache(uniqueIdInt, reply);
    }

    private void configureRouting() throws UnknownHostException {
        int ipIndex = requestLength - LENGTH_IP - LENGTH_PORT;
        int portIndex = requestLength - LENGTH_PORT;

        byte[] ip = new byte[LENGTH_IP];
        System.arraycopy(request, ipIndex, ip, 0, LENGTH_IP);

        int port = ByteUtil.leb2int(request, portIndex, LENGTH_PORT);

        packet.setAddress(InetAddress.getByAddress(ip));
        packet.setPort(port);

        if (Logger.VERBOSE_REQUEST)
            Logger.log(Logger.TAG_REQUEST, "configured routing to [" + packet.getAddress().getHostAddress() + ":" + port
                    + "]: " + uniqueIdInt);
    }

    private void convertToInternal() {
        reply = Reply.createInternalReply(packet);

        if (Logger.VERBOSE_REQUEST)
            Logger.log(Logger.TAG_REQUEST, "packet converted to command " + reply[LENGTH_UNIQUE_ID] + " to [" + packet.getAddress().getHostAddress()
                    + ": " + packet.getPort() + "]: " + uniqueIdInt);
    }

    /**
     * Get value from store with given key
     */
    private byte[] get() {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        byte[] value = database.get(keyInt);

        if (value == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, value);
    }

    /**
     * Parses the key to use in the command
     */
    private void parseKey() {
        if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "no key to parse");

            key = null;
            keyInt = null;
            return;
        }

        key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        keyInt = new BigInteger(key);
    }

    /**
     * Parses the unique id of the request
     */
    private void parseUniqueId() {
        if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "failed to parse unique id");

            uniqueId = null;
            uniqueIdInt = null;
            return;
        }

        uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);
        uniqueIdInt = new BigInteger(uniqueId);
    }

    /**
     * Put key-value pair into store
     */
    private byte[] put()  {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        // Checks if packet contains a value to put
        if (requestLength <= LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "request length of " + requestLength + " too short: " + uniqueIdInt);

            return Reply.createReply(packet, uniqueId, ERROR_VALUE);
        }

        int valueLength = ByteUtil.leb2int(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY, LENGTH_VALUE_LENGTH);
        if (valueLength < 1 || valueLength > LENGTH_VALUE) {
            // Checks if the length of the value to put is valid

            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "invalid value length of " + valueLength + ": " + uniqueIdInt);

            return Reply.createReply(packet, uniqueId, ERROR_LENGTH);
        } else if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + valueLength) {
            // Checks if the packet length is valid with the length of the value

            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "request length of " + requestLength + "does not match value length of "
                        + valueLength + ": " + uniqueIdInt);

            return Reply.createReply(packet, uniqueId, ERROR_VALUE);
        } else if (database.size() >= MAX_STORE) {
            // Checks if there is room in the database

            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "database at max capacity");

            return Reply.createReply(packet, uniqueId, ERROR_MEMORY);
        }

        byte[] value = new byte[valueLength];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH, value, 0, valueLength);

        database.put(keyInt, value);

        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }

    /**
     * Remove key-value pair from store
     */
    private byte[] remove() {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        byte[] removed = database.remove(keyInt);

        if (removed == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }
}
