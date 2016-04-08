package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.util.Logger;

import java.math.BigInteger;
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
     * Public function for Circle.getSelfKey()
     */
    public static BigInteger getSelfKey() {
        return nodes.getSelfKey();
    }

    /**
     * Public function for Circle.getSelfNode()
     */
    public static Node getSelfNode() {
        return nodes.getSelfNode();
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
