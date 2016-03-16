package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.distribution.Route;
import com.s26643114.CPEN431.system.Database;
import com.s26643114.CPEN431.util.ByteUtil;
//import com.s26643114.CPEN431.util.Logger;

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
    //private static final String TAG = "request";

    private byte[] key;
    private byte[] request;
    private byte[] reply;
    private byte[] uniqueId;

    private int requestLength;

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
        requestLength = packet.getLength();
        reply = null;
    }

    /**
     * Parses and completes the request
     */
    public DatagramPacket parse() throws IOException {
        parseKey();
        parseUniqueId();

        if (uniqueIdInt == null) {
            Reply.setReply(packet, ERROR_PACKET);
            return packet;
        }

        //Checks if request is in cache
        reply = database.check(uniqueIdInt);
        if (reply != null) {
            //if (Logger.VERBOSE_REQUEST)
                //Logger.log(TAG, "reply found in cache with unique id: " + uniqueIdInt);

            if (request[LENGTH_UNIQUE_ID] > MASK_COMMAND)
                configureRouting();

            Reply.setReply(packet, reply);
            return packet;
        }

        byte command = request[LENGTH_UNIQUE_ID];
        if (command <= COMMAND_REMOVE && !Route.checkSelf(key)) {
            convertToInternal();
            Route.route(packet, key);
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
                    //if (Logger.VERBOSE_REQUEST)
                        //Logger.log(TAG, "shutdown requested");

                    shutdown.set(true);
                    Reply.setReply(packet, ERROR_NONE);
                    return packet;
                case COMMAND_REMOVE_ALL:
                    //if (Logger.VERBOSE_REQUEST)
                        //Logger.log(TAG, "remove all request");

                    database.clear();
                    reply = Reply.createReply(packet, uniqueId, ERROR_NONE);
                    break;
                default:
                    //if (Logger.VERBOSE_REQUEST)
                        //Logger.log(TAG, "command not found: " + command);

                    reply = Reply.createReply(packet, uniqueId, ERROR_COMMAND);
                    break;
            }

            cache();
            return packet;
        }
    }

    /**
     * Rejects the request with the error code
     */
    public DatagramPacket reject(byte errorCode) throws UnknownHostException {
        if (request[LENGTH_UNIQUE_ID] > MASK_COMMAND)
            configureRouting();

        //if (Logger.VERBOSE_REQUEST)
            //Logger.log(TAG, "rejecting package with code " + errorCode);

        Reply.setReply(packet, errorCode);

        return packet;
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
        Route.ack(packet);

        int ipIndex = requestLength - LENGTH_IP - LENGTH_PORT - LENGTH_INSTANT;
        int portIndex = requestLength - LENGTH_PORT - LENGTH_INSTANT;

        byte[] ip = new byte[LENGTH_IP];
        System.arraycopy(request, ipIndex, ip, 0, LENGTH_IP);

        int port = ByteUtil.leb2int(request, portIndex, LENGTH_PORT);

        packet.setAddress(InetAddress.getByAddress(ip));
        packet.setPort(port);

        //if (Logger.VERBOSE_REQUEST)
            //Logger.log(TAG, "configured routing to [" + packet.getAddress().getHostAddress() + ":" + port + "]");
    }

    private void convertToInternal() {
        reply = Reply.createInternalReply(packet, System.nanoTime());

        //if (Logger.VERBOSE_REQUEST)
            //Logger.log(TAG, "packet converted to command " + reply[LENGTH_UNIQUE_ID] + ", ip " + packet.getAddress().getHostAddress()
                    //+ ", port " + packet.getPort() + " and packet length " + packet.getLength());
    }

    /**
     * Get value from store with given key
     */
    private byte[] get() {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        byte[] value = database.get(keyInt);

        //if (Logger.VERBOSE_REQUEST) {
            //if (value == null)
                //Logger.log(TAG, "get key not found: " + keyInt);
            //else
                //Logger.log(TAG, "get [" + keyInt + ": " + value.length + "]");
        //}

        if (value == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, value);
    }

    /**
     * Parses the key to use in the command
     */
    private void parseKey() {
        if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY) {
            //if (Logger.VERBOSE_REQUEST)
                //System.out.println("no key to parse");

            key = null;
            keyInt = null;
            return;
        }

        key = new byte[LENGTH_KEY];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE, key, 0, LENGTH_KEY);
        keyInt = new BigInteger(key);

        //if (Logger.VERBOSE_REQUEST)
            //Logger.log(TAG, "key parsed: " + keyInt);
    }

    /**
     * Parses the unique id of the request
     */
    private void parseUniqueId() {
        if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE) {
            //if (Logger.VERBOSE_REQUEST)
                //System.out.println("no unique id to parse");

            uniqueId = null;
            uniqueIdInt = null;
            return;
        }

        uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);
        uniqueIdInt = new BigInteger(uniqueId);

        //if (Logger.VERBOSE_REQUEST)
            //Logger.log(TAG, "unique id parsed: " + uniqueIdInt);
    }

    /**
     * Put key-value pair into store
     */
    private byte[] put()  {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        // Checks if packet contains a value to put
        if (requestLength <= LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH)
            return Reply.createReply(packet, uniqueId, ERROR_VALUE);

        int valueLength = ByteUtil.leb2int(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY, LENGTH_VALUE_LENGTH);

        // Checks if the length of the value to put is valid
        if (valueLength < 1 || valueLength > LENGTH_VALUE)
            return Reply.createReply(packet, uniqueId, ERROR_LENGTH);

        // Checks if the packet length is valid with the length of the value
        else if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + valueLength)
            return Reply.createReply(packet, uniqueId, ERROR_VALUE);

        // Checks if there is room in the database
        else if (database.size() >= MAX_STORE)
            return Reply.createReply(packet, uniqueId, ERROR_MEMORY);

        byte[] value = new byte[valueLength];
        System.arraycopy(request, LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH, value, 0, valueLength);

        database.put(keyInt, value);

        //if (Logger.VERBOSE_REQUEST)
            //Logger.log(TAG, "put [" + keyInt + ":" + valueLength + "]");

        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }

    /**
     * Remove key-value pair from store
     */
    private byte[] remove() {
        if (keyInt == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        byte[] removed = database.remove(keyInt);

        //if (Logger.VERBOSE_REQUEST) {
            //if (removed == null)
                //Logger.log(TAG, "remove key not found: " + keyInt);
            //else
                //Logger.log(TAG, "remove [" + keyInt + ":" + removed.length + "]");
        //}

        if (removed == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }
}
