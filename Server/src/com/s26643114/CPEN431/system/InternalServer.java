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
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternalServer extends Thread {
    private long end;
    private long start;

    private AtomicBoolean started;
    private AtomicBoolean shutdown;
    private DatagramSocket internalServer;
    private Heartbeat heartbeatThread;
    private Map<BigInteger, Node> heartbeats;
    private Node self;

    public InternalServer(AtomicBoolean shutdown, AtomicBoolean started, InetAddress ip, int port) throws SocketException {
        this.shutdown = shutdown;
        this.started = started;
        internalServer = new DatagramSocket(port + 1, ip);
        internalServer.setReceiveBufferSize(Protocol.SIZE_BUFFER);

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "receive buffer: " + internalServer.getReceiveBufferSize());

        heartbeats = Route.getHeartbeats();
        self = Route.getSelf();

        heartbeatThread = new Heartbeat(started, internalServer, heartbeats);
        new Thread(heartbeatThread).start();
    }

    @Override
    public void interrupt() {
        super.interrupt();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "stopping heartbeat thread");
        heartbeatThread.stop();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "shutting down internal server socket");

        internalServer.close();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "stopped");
    }

    @Override
    public void run() {
        Route.initNodes();

        byte[] data = new byte[heartbeats.size() * Protocol.LENGTH_HEARTBEAT];
        DatagramPacket packet = new DatagramPacket(data, heartbeats.size() * Protocol.LENGTH_HEARTBEAT);
        while(!shutdown.get()) {
            try {
                self.increment();
                Node node = Route.getNext();
                packet.setAddress(node.getIp());
                packet.setPort(node.getPort() + 1);

                int i = 0;
                for (BigInteger key : heartbeats.keySet()) {
                    Node n = heartbeats.get(key);

                    System.arraycopy(n.getIp().getAddress(), 0, data, i * Protocol.LENGTH_HEARTBEAT, Protocol.LENGTH_IP);
                    ByteUtil.longToByteArray(data, n.getHeartbeat(), i * Protocol.LENGTH_HEARTBEAT + Protocol.LENGTH_IP);

                    i++;
                }

                packet.setLength(i * Protocol.LENGTH_HEARTBEAT);

                if (Logger.BENCHMARK_INTERNAL)
                    start = System.nanoTime();

                internalServer.send(packet);

                if (Logger.BENCHMARK_INTERNAL) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_INTERNAL, start, end, "receive");
                }

                if (Logger.VERBOSE_INTERNAL)
                    Logger.log(Logger.TAG_INTERNAL, "heartbeats sent to node [" + packet.getAddress().getHostAddress()
                            + ":" + packet  .getPort() + "]");

                Route.checkNodes();
                sleep(Protocol.TIME_HEARTBEAT);
            } catch (IOException | InterruptedException e) {
                if (Logger.VERBOSE_INTERNAL)
                    Logger.log(Logger.TAG_INTERNAL, e);
            }
        }

        heartbeatThread.stop();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "stopped");
    }

    @Override
    public synchronized void start() {
        started.set(true);
        super.start();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "started");
    }

    private class Heartbeat implements Runnable {
        private volatile boolean running;
        private int size;

        private AtomicBoolean started;
        private DatagramPacket packet;
        private DatagramSocket internalServer;
        private Map<BigInteger, Node> heartbeats;

        public Heartbeat(AtomicBoolean started, DatagramSocket internalServer, Map<BigInteger, Node> heartbeats) {
            this.started = started;
            this.internalServer = internalServer;
            this.heartbeats = heartbeats;
            size = heartbeats.size() * Protocol.LENGTH_HEARTBEAT;
            packet = new DatagramPacket(new byte[size], size);

            running = true;
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            if (Logger.VERBOSE_INTERNAL)
                Logger.log(Logger.TAG_INTERNAL, "starting heartbeat receive");

            byte[] ip = new byte[Protocol.LENGTH_IP];
            while (running) {
                try {
                    packet.setLength(size);
                    internalServer.receive(packet);

                    if (!started.get()) {
                        if (Logger.VERBOSE_INTERNAL)
                            Logger.log(Logger.TAG_INTERNAL, "starting internal server because of heartbeats from ["
                                    + packet.getAddress().getHostAddress() + ":" + packet.getPort() + "]");
                        start();
                    }

                    if (Logger.VERBOSE_INTERNAL)
                        Logger.log(Logger.TAG_INTERNAL, "heartbeats received from node [" + packet.getAddress().getHostAddress()
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
                    if (Logger.VERBOSE_INTERNAL)
                        Logger.log(Logger.TAG_INTERNAL, e);
                }
            }

            if (Logger.VERBOSE_INTERNAL)
                Logger.log(Logger.TAG_INTERNAL, "heartbeat stopped");
        }
    }
}
