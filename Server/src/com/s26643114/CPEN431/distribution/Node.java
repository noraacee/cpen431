package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;

import java.math.BigInteger;
import java.net.InetAddress;

public class Node extends Protocol {
    private int port;

    private long heartbeat;
    private long lastSeen;

    private BigInteger key;
    private InetAddress ip;

    public Node(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;

        heartbeat = 0;
    }

    public void heartbeat(long counter) {
        if (counter > heartbeat) {
            heartbeat = counter;
            lastSeen = System.currentTimeMillis();
        }
    }

    public void increment() {
        heartbeat++;
        lastSeen = System.currentTimeMillis();
    }

    public void init() {
        lastSeen = System.currentTimeMillis();
    }

    public boolean isAlive() {
        return System.currentTimeMillis() - lastSeen <= TIME_FAILURE;
    }

    public long getHeartbeat() {
        return heartbeat;
    }

    public InetAddress getIp() {
        return ip;
    }

    public BigInteger getKey() {
        return key;
    }

    public int getPort() {
        return port;
    }

    public void setKey(BigInteger key) {
        this.key = key;
    }
}
