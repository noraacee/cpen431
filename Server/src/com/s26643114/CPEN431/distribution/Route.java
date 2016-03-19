package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.system.Server;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.util.Map;

public class Route extends Protocol {
    private static Circle nodes;
    private static Server server;

    public static void init(String ip, String nodesFileName) throws Exception {
        nodes = new Circle(ip, nodesFileName);

        if (Logger.VERBOSE_ROUTE)
            Logger.log(Logger.TAG_ROUTE, "initialized");
    }

    public static void checkNodes() {
        nodes.checkNodes();
    }

    public static boolean checkSelf(byte[] key) {
        return nodes.checkSelf(key);
    }

    public static Map<BigInteger, Node> getHeartbeats() {
        return nodes.getHeartbeats();
    }

    public static Node getNext() {
        return nodes.getNextNode();
    }

    public static Node getSelf() {
        return nodes.getSelf();
    }

    public static void initNodes() {
        nodes.initNodes();
    }

    public static void route(DatagramPacket packet, byte[] requestKey) {
        Node node;
        while (true) {
            long start;
            if (Logger.BENCHMARK_ROUTE)
                start = System.nanoTime();

            BigInteger key = nodes.getNodeKey(requestKey);

            if (Logger.BENCHMARK_ROUTE) {
                long end = System.nanoTime();
                Logger.benchmark(Logger.TAG_ROUTE, start, end, "node key");
            }

            node = nodes.getNode(key);
            if (node != null)
                break;
        }

        packet.setAddress(node.getIp());
        packet.setPort(node.getPort());

        if (Logger.VERBOSE_ROUTE)
            Logger.log(Logger.TAG_ROUTE, "routing to [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

        int retries = RETRIES;
        while (retries > 0) {
            try {
                server.send(packet);
                return;
            } catch (IOException e) {
                retries--;

                if (Logger.VERBOSE_ROUTE)
                    Logger.log(Logger.TAG_ROUTE, e);
            }
        }
    }

    public static void setServer(Server server) {
        Route.server = server;
    }
}
