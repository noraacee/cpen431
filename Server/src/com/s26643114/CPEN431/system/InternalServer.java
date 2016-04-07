package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Node;
import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Reply;
import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executor;

public class InternalServer extends Thread {
    public volatile boolean running;

    private Database database;
    private DatagramSocket internalServer;
    private Executor executor;
    private ReplicationServer replicationServer;
    private Server server;

    public InternalServer(Server server, Executor executor, Database database,  InetAddress ip, int port) throws IOException {
        this.server = server;
        this.executor = executor;
        this.database = database;
        running = false;

        internalServer = new DatagramSocket(port + Protocol.PORT_INTERNAL, ip);
        internalServer.setReceiveBufferSize(Protocol.SIZE_BUFFER);

        replicationServer = new ReplicationServer(server, executor, database, ip, port);

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "receive buffer: " + internalServer.getReceiveBufferSize());
    }

    @Override
    public void run() {
        running = true;

        replicationServer.start();

        long start;
        long end;
        while(running) {
            if (Logger.BENCHMARK_INTERNAL)
                start = System.nanoTime();

            Client client = database.poll(server);
            client.setMode(Request.Mode.INTERNAL);
            DatagramPacket packet = client.getPacket();

            if (Logger.BENCHMARK_INTERNAL) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_INTERNAL, start, end, "client");
                start = System.nanoTime();
            }

            try {
                internalServer.receive(packet);

                if (Logger.BENCHMARK_INTERNAL) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_INTERNAL, start, end, "receive");
                    start = System.nanoTime();
                }

                executor.execute(client);

                if (Logger.BENCHMARK_INTERNAL) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_INTERNAL, start, end, "start client");
                }
            } catch (OutOfMemoryError e) {
                Reply.setReply(client.getPacket(), Protocol.ERROR_MEMORY);

                try {
                    internalServer.send(client.getPacket());
                } catch (IOException ioe) {
                    if (Logger.VERBOSE_INTERNAL)
                        Logger.log(Logger.TAG_INTERNAL, ioe);
                }
            } catch (IOException e) {
                if (Logger.VERBOSE_INTERNAL)
                    Logger.log(Logger.TAG_INTERNAL, e);
            }
        }

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "stopped");
    }

    public void exit() {
        running = false;
        internalServer.close();
        replicationServer.exit();
    }

    public void send(DatagramPacket packet, boolean replicate, Node[] replicas) throws IOException {
        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "sending to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort() + "]: " + packet.getLength());

        long start;
        if (Logger.BENCHMARK_INTERNAL)
            start = System.nanoTime();

        internalServer.send(packet);

        if (Logger.BENCHMARK_INTERNAL) {
            long end = System.nanoTime();
            Logger.benchmark(Logger.TAG_INTERNAL, start, end, "send");
        }

        if (replicate)
            sendReplicas(packet, replicas);
    }

    public void sendReplicas(DatagramPacket packet, Node[] replicas) throws IOException {
        for (int i = 1; i < Protocol.REPLICATION; i++)
            replicationServer.send(packet, replicas[i]);
    }
}
