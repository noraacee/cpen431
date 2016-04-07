package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;

import java.math.BigInteger;
import java.net.InetAddress;

public class Node {
    private int port; // immutable

    private long heartbeat;
    private long lastSeen;

    private BigInteger heartbeatKey; // immutable
    private BigInteger nodeKey; // immutable
    private InetAddress ip; // immutable

    public Node(InetAddress ip, int port, BigInteger nodeKey, BigInteger heartbeatKey) {
        this.ip = ip;
        this.port = port;
        this.nodeKey = nodeKey;
        this.heartbeatKey = heartbeatKey;

        heartbeat = 0;
    }

    /**
     * Checks if heartbeat has been incremented, and if so, record the time
     */
    public void heartbeat(long counter) {
        if (counter > heartbeat) {
            heartbeat = counter;
            lastSeen = System.currentTimeMillis();
        }
    }

    /**
     * Increment this node's heartbeat
     */
    public void increment() {
        heartbeat++;
        lastSeen = System.currentTimeMillis();
    }

    /**
     * Initializes the heartbeat timer
     */
    public void init() {
        lastSeen = System.currentTimeMillis();
    }

    /**
     * Checks if node is alive based on the last time a heartbeat was recorded
     */
    public boolean isAlive() {
        return System.currentTimeMillis() - lastSeen <= Protocol.TIME_FAILURE;
    }

    /**
     * Getter for the node heartbeat
     */
    public long getHeartbeat() {
        return heartbeat;
    }

    /**
     * Getter for the node heartbeat nodeKey
     */
    public BigInteger getHeartbeatKey() {
        return heartbeatKey;
    }

    /**
     * Getter for the node ip
     */
    public InetAddress getIp() {
        return ip;
    }


    /**
     * Geter for the node nodeKey
     */
    public BigInteger getNodeKey() {
        return nodeKey;
    }

    /**
     * Getter for the node port
     */
    public int getPort() {
        return port;
    }
}
