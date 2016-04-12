package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.distribution.Node;
import com.s26643114.CPEN431.distribution.Route;
import com.s26643114.CPEN431.system.Database;
import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Encapsulates a request and its reply
 */
public class Request extends Protocol {
    public enum Mode {
        EXTERNAL, INTERNAL, REPLICATION
    }

    protected boolean replicate;
    protected boolean shutdown;

    protected Node[] replicas;

    protected BigInteger nodeKey;
    protected Database database;
    protected Mode mode;

    private boolean self;

    private byte[] key;
    private byte[] request;
    private byte[] reply;
    private byte[] uniqueId;

    private int count;
    private int requestLength;

    long end;
    long start;

    private BigInteger keyInt;
    private BigInteger uniqueIdInt;
    private DatagramPacket packet;

    public Request(Database database) {
        this.database = database;

        replicate = false;
        shutdown = false;
        replicas = new Node[REPLICATION];
        nodeKey = Route.getSelfKey();
        mode = Mode.EXTERNAL;

        self = false;
        request = new byte[LENGTH_TOTAL];
        count = 0;
        packet = new DatagramPacket(request, LENGTH_TOTAL);
        reply = null;
    }

    /**
     * Gets the packet associated with this request
     */
    public DatagramPacket getPacket() {
        return packet;
    }

    /**
     * Initializes the request for a new client
     */
    public void reset() {
        replicate = false;
        mode = Mode.EXTERNAL;

        self = false;
        packet.setData(request);
        reply = null;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Rejects the request with the error code
     */
    protected DatagramPacket createReject(byte errorCode) throws UnknownHostException {
        if (mode == Mode.INTERNAL)
            configureRouting();

        Reply.setReply(packet, errorCode);

        return packet;
    }

    protected boolean init() {
        requestLength = packet.getLength();
        parseKey();
        parseUniqueId();

        if (uniqueIdInt == null)
            return false;

        if (Logger.BENCHMARK_REQUEST)
            start = System.nanoTime();

        reply = database.check(uniqueIdInt);

        if (Logger.BENCHMARK_REQUEST) {
            end = System.nanoTime();
            Logger.benchmark(Logger.TAG_REQUEST, start, end, "check cache");
        }

        if (reply != null)
            return false;


        byte command = request[LENGTH_UNIQUE_ID];
        if (command <= COMMAND_REMOVE) {
            Route.route(key, replicas);

            for (int i = 0; i < Protocol.REPLICATION; i++) {
                if (replicas[i].getNodeKey().equals(nodeKey)) {
                    replicas[i] = null;
                    self = true;
                    break;
                }
            }

            if (mode == Mode.EXTERNAL && self) {
                if (command == COMMAND_PUT || command == COMMAND_REMOVE) {
                    if (Logger.VERBOSE_REQUEST) {
                        for (int i = 0; i < Protocol.REPLICATION; i++) {
                        if (replicas[i] == null)
                            Logger.log(Logger.TAG_REQUEST, "self: " + uniqueIdInt);
                        else
                            Logger.log(Logger.TAG_REQUEST, "replicating to [" + replicas[i].getIp().getHostAddress() + ":"
                                    + replicas[i].getPort() + "]: " + uniqueIdInt);
                    }
                }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Parses and completes the request
     */
    protected DatagramPacket parse() throws IOException {
        if (uniqueIdInt == null) {
            Reply.setReply(packet, ERROR_PACKET);
            return packet;
        }

        if (reply != null) {
            if (mode == Mode.REPLICATION)
                return packet;
            if (mode == Mode.INTERNAL)
                configureRouting();

            Reply.setReply(packet, reply);

            return packet;
        }

        if (Logger.BENCHMARK_REQUEST)
            start = System.nanoTime();

        byte command = request[LENGTH_UNIQUE_ID];

        if (mode != Mode.INTERNAL && mode != Mode.REPLICATION && command <= COMMAND_REMOVE && !self) {
            mode = Mode.INTERNAL;
            reply = Reply.createInternalReply(packet);

            if (command == COMMAND_GET) {
                packet.setAddress(replicas[0].getIp());
                packet.setPort(replicas[0].getPort()+ PORT_INTERNAL);

                count++;
                if (count == REPLICATION)
                    count = 0;
            } else {
                if (Logger.VERBOSE_REQUEST) {
                    for (int i = 0; i < REPLICATION; i++)
                        Logger.log(Logger.TAG_REQUEST, "replicating to [" + replicas[i].getIp().getHostAddress() + ":"
                                + replicas[i].getPort() + "]: " + uniqueIdInt);
                }

                packet.setAddress(replicas[0].getIp());
                packet.setPort(replicas[0].getPort() + PORT_INTERNAL);
                replicas[0] = null;
                replicate = true;
            }

            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "routed to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort() + "]");


            if (Logger.BENCHMARK_REQUEST) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_REQUEST, start, end, "route");
            }

            return packet;
        } else {
            if (mode == Mode.INTERNAL)
                configureRouting();

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

                    shutdown = true;

                    Reply.setReply(packet, ERROR_NONE);
                    return packet;
                case COMMAND_REMOVE_ALL:
                    database.clear();
                    Reply.setReply(packet, ERROR_NONE);
                    return packet;
                case COMMAND_PID:
                    Reply.setReply(packet, PID);
                    return packet;
                default:
                    if (Logger.VERBOSE_REQUEST)
                        Logger.log(Logger.TAG_REQUEST, "command " + command + " not found: " + uniqueIdInt);

                    reply = Reply.createReply(packet, uniqueId, ERROR_COMMAND);
                    break;
            }

            if (Logger.BENCHMARK_REQUEST) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_REQUEST, start, end, "process command");
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

    /**
     * Configures an internally routed request to return to the original sender
     */
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

    /**
     * Get value from store with given key
     */
    private byte[] get() {
        if (keyInt == null) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "no key for get");
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        }

        byte[] value = database.get(keyInt);

        if (value == null) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "cannot get key: " + keyInt);

            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        }
        return Reply.createReply(packet, uniqueId, value);
    }

    /**
     * Parses the key to use in the command
     */
    private void parseKey() {
        if (requestLength < LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY) {
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
        if (keyInt == null) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "no key for put");
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        }

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

        if (Logger.VERBOSE_REQUEST)
            Logger.log(Logger.TAG_REQUEST, "put [" + uniqueIdInt + ":" + keyInt + "]");

        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }

    /**
     * Remove key-value pair from store
     */
    private byte[] remove() {
        if (keyInt == null) {
            if (Logger.VERBOSE_REQUEST)
                Logger.log(Logger.TAG_REQUEST, "no key for remove");
            return Reply.createReply(packet, uniqueId, ERROR_KEY);
        }

        byte[] removed = database.remove(keyInt);

        if (removed == null)
            return Reply.createReply(packet, uniqueId, ERROR_KEY);

        if (Logger.VERBOSE_REQUEST)
            Logger.log(Logger.TAG_REQUEST, "remove: " + keyInt);

        return Reply.createReply(packet, uniqueId, ERROR_NONE);
    }
}
