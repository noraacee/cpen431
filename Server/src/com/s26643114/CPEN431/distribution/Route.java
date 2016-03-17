package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.system.Server;
import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Route extends Protocol {
    private static BigInteger self;
    private static Circle nodes;
    private static Server server;

    public static void init(Server server, String nodesFileName) throws Exception {
        Route.server = server;

        nodes = new Circle(nodesFileName);
        self = nodes.getKey(server.getAddress());

        if (Logger.VERBOSE_ROUTE)
            Logger.log(Logger.TAG_ROUTE, "initialized");
    }

    public static void ack(DatagramPacket packet) {
        byte[] ack = new byte[LENGTH_INSTANT];
        System.arraycopy(packet.getData(), packet.getLength() - LENGTH_INSTANT, ack, 0, LENGTH_INSTANT);

        packet.setData(ack);
        packet.setLength(LENGTH_INSTANT);

        if (Logger.VERBOSE_ROUTE)
            Logger.log(Logger.TAG_ROUTE, "acknowledging [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "] with lock key: " + ByteUtil.byteArrayToLong(ack, 0));

        try {
            server.send(packet);
        } catch (IOException e) {
            if (Logger.VERBOSE_ROUTE)
                Logger.log(Logger.TAG_ROUTE, e);
        }
    }

    public static long convertKey(DatagramPacket packet) {
        return ByteUtil.byteArrayToLong(packet.getData(), packet.getLength() - LENGTH_INSTANT);
    }

    public static boolean checkSelf(byte[] key) {
        return key == null || self.equals(nodes.getNodeKey(key));
    }

    public static void route(DatagramPacket packet, byte[] requestKey) {
        long start;
        long end;

        long lockKey = convertKey(packet);

        while (true) {
            if (Logger.BENCHMARK_ROUTE)
                start = System.nanoTime();

            BigInteger key = nodes.getNodeKey(requestKey);

            if (Logger.BENCHMARK_ROUTE) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_ROUTE, start, end, "node key");
            }

            Node node = nodes.getNode(key);

            packet.setAddress(node.getIp());
            packet.setPort(node.getPort());

            if (Logger.VERBOSE_ROUTE)
                Logger.log(Logger.TAG_ROUTE, "routing to [" + node.getIp().getHostAddress() + ":" + node.getPort()
                        + "] with lock key: " + lockKey);

            try {
                int retries = RETRIES;
                long timeout = TIMEOUT / (1 + 2 + 4);
                AtomicBoolean lock = new AtomicBoolean();
                while (retries > 0) {
                    synchronized (server.sendInternal(packet, lockKey, lock)) {
                        long instant = System.currentTimeMillis();
                        try {
                            lock.wait(timeout);
                            if (System.currentTimeMillis() - instant < timeout) {
                                if (Logger.VERBOSE_ROUTE)
                                    Logger.log(Logger.TAG_ROUTE, "route acknowledged from [" + node.getIp().getHostAddress()
                                            + ":" + node.getPort() + "] with lock key: " + lockKey);
                                return;
                            }
                        } catch (InterruptedException e) {
                            if (Logger.VERBOSE_ROUTE)
                                Logger.log(Logger.TAG_ROUTE, "route acknowledged from [" + node.getIp().getHostAddress()
                                        + ":" + node.getPort() + "] with lock key: " + lockKey);
                            return;
                        }
                    }

                    if (Logger.VERBOSE_ROUTE)
                        Logger.log(Logger.TAG_ROUTE, "retrying route to [" + node.getIp().getHostAddress() + ":" + node.getPort()
                                + "] with lock key: " + lockKey);
                    timeout *= 2;
                    retries--;
                }

                if (Logger.VERBOSE_ROUTE)
                    Logger.log(Logger.TAG_ROUTE, "rerouting because of crashed node: [" + node.getIp().getHostAddress() + ":"
                            + node.getPort() + "] with lock key: " + lockKey);

                server.removeInternal(lockKey);
                nodes.remove(key);
            } catch (IOException e) {
                if (Logger.VERBOSE_ROUTE)
                    Logger.log(Logger.TAG_ROUTE, e);

                server.removeInternal(lockKey);
            }
        }
    }
}
