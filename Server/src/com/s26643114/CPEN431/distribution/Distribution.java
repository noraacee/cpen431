package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.system.Server;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;

public class Distribution extends Protocol {
    private static BigInteger self;
    private static Circle nodes;
    private static Server server;

    public static void init(Server server, String nodesFileName) throws Exception {
        Distribution.server = server;

        nodes = new Circle(nodesFileName);
        self = nodes.getKey(server.getAddress());
    }

    public static void route(Request request) {
        if (Logger.VERBOSE_DISTRIBUTION)
            Logger.log("routing requested");

        BigInteger key = nodes.getNodeKey(request.getKey());
        Node node = nodes.getNode(key);
        DatagramPacket packet = request.convertToInternal(node.getIp(), node.getPort());

        try {
            server.send(packet);
        } catch (IOException e) {
            nodes.remove(key);
            route(request);
        }
    }

    public static boolean checkSelf(byte[] key) {
        return key == null || self.equals(nodes.getNodeKey(key));
    }
}
