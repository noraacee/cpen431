package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Node;
import com.s26643114.CPEN431.distribution.Route;
import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public class HeartbeatServer extends Thread {
    private volatile boolean running;

    private DatagramSocket heartbeatServer;
    private Heartbeat heartbeatThread;
    private Map<BigInteger, Node> heartbeats;
    private Node self;

    public HeartbeatServer(InetAddress ip, int port) throws IOException {
        running = false;

        heartbeatServer = new DatagramSocket(port + Protocol.PORT_HEARTBEAT, ip);
        heartbeatServer.setReceiveBufferSize(Protocol.SIZE_BUFFER);

        if (Logger.VERBOSE_HEARTBEAT)
            Logger.log(Logger.TAG_HEARTBEAT, "receive buffer: " + heartbeatServer.getReceiveBufferSize());

        heartbeats = Route.getHeartbeats();
        self = Route.getSelf();

        heartbeatThread = new Heartbeat(heartbeatServer, heartbeats);
        new Thread(heartbeatThread).start();
    }

    /**
     * Stops the heartbeat thread and closes the server socket
     */
    @Override
    public void interrupt() {
        super.interrupt();

        if (Logger.VERBOSE_HEARTBEAT)
            Logger.log(Logger.TAG_HEARTBEAT, "stopping heartbeat thread");
        heartbeatThread.stop();

        if (Logger.VERBOSE_HEARTBEAT)
            Logger.log(Logger.TAG_HEARTBEAT, "shutting down internal server socket");

        heartbeatServer.close();

        if (Logger.VERBOSE_HEARTBEAT)
            Logger.log(Logger.TAG_HEARTBEAT, "stopped");
    }

    /**
     * Sends current heartbeats to a new node at a set interval while incrementing its own heartbeat at every interval
     */
    @Override
    public void run() {
        running = true;
        Route.initHeartbeats();

        byte[] data = new byte[heartbeats.size() * Protocol.LENGTH_HEARTBEAT];
        DatagramPacket packet = new DatagramPacket(data, heartbeats.size() * Protocol.LENGTH_HEARTBEAT);
        while(running) {
            try {
                self.increment();
                Node node = Route.getNext();
                packet.setAddress(node.getIp());
                packet.setPort(node.getPort() + Protocol.PORT_HEARTBEAT);

                int i = 0;
                for (Map.Entry<BigInteger, Node> entry: heartbeats.entrySet()) {
                    Node n = entry.getValue();
                    System.arraycopy(n.getIp().getAddress(), 0, data, i, Protocol.LENGTH_IP);
                    ByteUtil.longToByteArray(data, n.getHeartbeat(), i + Protocol.LENGTH_IP);

                    i += Protocol.LENGTH_HEARTBEAT;
                }

                packet.setLength(i);

                long start;
                if (Logger.BENCHMARK_HEARTBEAT)
                    start = System.nanoTime();

                heartbeatServer.send(packet);

                if (Logger.BENCHMARK_HEARTBEAT) {
                    long end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_HEARTBEAT, start, end, "receive");
                }

                if (Logger.VERBOSE_HEARTBEAT)
                    Logger.log(Logger.TAG_HEARTBEAT, "heartbeats sent to node [" + packet.getAddress().getHostAddress()
                            + ":" + packet  .getPort() + "]");

                Route.checkNodes();
                sleep(Protocol.TIME_HEARTBEAT);
            } catch (IOException | InterruptedException e) {
                if (Logger.VERBOSE_HEARTBEAT)
                    Logger.log(Logger.TAG_HEARTBEAT, e);
            }
        }

        heartbeatThread.stop();

        if (Logger.VERBOSE_HEARTBEAT)
            Logger.log(Logger.TAG_HEARTBEAT, "stopped");
    }

    /**
     * Starts the internal server thread
     */
    @Override
    public void start() {
        if (!running) {
            try {
                super.start();
            } catch (IllegalThreadStateException ignored) {
            }

            if (Logger.VERBOSE_HEARTBEAT)
                Logger.log(Logger.TAG_HEARTBEAT, "started");
        }
    }

    public void exit() {
        running = false;
        heartbeatServer.close();
    }

    private class Heartbeat implements Runnable {
        private volatile boolean running;
        private int size;

        private DatagramPacket packet;
        private DatagramSocket internalServer;
        private Map<BigInteger, Node> heartbeats;

        public Heartbeat(DatagramSocket internalServer, Map<BigInteger, Node> heartbeats) {
            this.internalServer = internalServer;
            this.heartbeats = heartbeats;
            size = heartbeats.size() * Protocol.LENGTH_HEARTBEAT;
            packet = new DatagramPacket(new byte[size], size);

            running = false;
        }

        /**
         * Stops the heartbeat thread
         */
        public void stop() {
            running = false;
        }

        /**
         * Receives heartbeats from other nodes and updates its own list of heartbeats
         */
        @Override
        public void run() {
            running = true;

            if (Logger.VERBOSE_HEARTBEAT)
                Logger.log(Logger.TAG_HEARTBEAT, "starting heartbeat receive");

            byte[] ip = new byte[Protocol.LENGTH_IP];
            while (running) {
                try {
                    packet.setLength(size);
                    internalServer.receive(packet);

                    start();

                    if (Logger.VERBOSE_HEARTBEAT)
                        Logger.log(Logger.TAG_HEARTBEAT, "heartbeats received from node [" + packet.getAddress().getHostAddress()
                                + ":" + packet.getPort() + "]");

                    byte[] data = packet.getData();
                    for (int i = 0; i < packet.getLength(); i += Protocol.LENGTH_HEARTBEAT) {
                        System.arraycopy(data, i, ip, 0, Protocol.LENGTH_IP);
                        BigInteger key = new BigInteger(ip);

                        Node n = heartbeats.get(key);
                        if (n != null)
                            n.heartbeat(ByteUtil.byteArrayToLong(data, i + Protocol.LENGTH_IP));
                    }
                } catch (IOException e) {
                    if (Logger.VERBOSE_HEARTBEAT)
                        Logger.log(Logger.TAG_HEARTBEAT, e);
                }
            }

            if (Logger.VERBOSE_HEARTBEAT)
                Logger.log(Logger.TAG_HEARTBEAT, "heartbeat stopped");
        }
    }
}
