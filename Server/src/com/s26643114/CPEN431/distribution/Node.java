package com.s26643114.CPEN431.distribution;

import java.net.InetAddress;

public class Node {
    private int port;
    private InetAddress ip;

    public Node(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
