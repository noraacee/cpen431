package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.util.Logger;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.util.Map;

public class Route {
    private static Circle nodes;

    /**
     * Initializes the Circle to be used in routing
     */
    public static void init(String ip, String nodesFileName) throws Exception {
        nodes = new Circle(ip, nodesFileName);

        if (Logger.VERBOSE_ROUTE)
            Logger.log(Logger.TAG_ROUTE, "initialized");
    }

    /**
     * Public function for Circle.checkNodes()
     */
    public static void checkNodes() {
        nodes.checkNodes();
    }

    /**
     * Public function for Circle.checkSelf()
     */
    public static boolean checkSelf(byte[] key) {
        return nodes.checkSelf(key);
    }

    /**
     * Public function for Circle.getHeartbeats()
     */
    public static Map<BigInteger, Node> getHeartbeats() {
        return nodes.getHeartbeats();
    }

    /**
     * Public function for Circle.getNextNode()
     */
    public static Node getNext() {
        return nodes.getNextNode();
    }

    /**
     * Public function for Circle.getSelf()
     */
    public static Node getSelf() {
        return nodes.getSelf();
    }

    /**
     * Public function for Circle.initNodes()
     */
    public static void initHeartbeats() {
        nodes.initHeartbeats();
    }

    /**
     * Routes the request to the proper node based on the hash of the key in the request
     */
    public static void route(DatagramPacket packet, byte[] requestKey, Node[] replicas) {
        Node node;
        while (true) {
            long start;
            if (Logger.BENCHMARK_ROUTE)
                start = System.nanoTime();

            node = nodes.getNode(requestKey, replicas);

            if (Logger.BENCHMARK_ROUTE) {
                long end = System.nanoTime();
                Logger.benchmark(Logger.TAG_ROUTE, start, end, "nodes");
            }

            if (node != null)
                break;
        }

        packet.setAddress(node.getIp());
        packet.setPort(node.getPort() + Protocol.PORT_INTERNAL);

        if (Logger.VERBOSE_ROUTE)
            Logger.log(Logger.TAG_ROUTE, "routed to [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");
    }

    public static void route(byte[] requestKey, Node[] replicas) {
        while (true) {
            long start;
            if (Logger.BENCHMARK_ROUTE)
                start = System.nanoTime();

            if (nodes.getNode(requestKey, replicas) != null)
                break;

            if (Logger.BENCHMARK_ROUTE) {
                long end = System.nanoTime();
                Logger.benchmark(Logger.TAG_ROUTE, start, end, "nodes");
            }
        }
    }
}
