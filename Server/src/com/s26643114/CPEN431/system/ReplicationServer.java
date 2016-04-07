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

public class ReplicationServer extends Thread {
    public volatile boolean running;

    private Database database;
    private DatagramSocket replicationServer;
    private Executor executor;
    private Server server;

    public ReplicationServer(Server server, Executor executor, Database database, InetAddress ip, int port) throws IOException {
        this.server = server;
        this.executor = executor;
        this.database = database;
        running = false;

        replicationServer = new DatagramSocket(port + Protocol.PORT_REPLICATION, ip);
        replicationServer.setReceiveBufferSize(Protocol.SIZE_BUFFER);

        if (Logger.VERBOSE_REPLICATION)
            Logger.log(Logger.TAG_REPLICATION, "receive buffer: " + replicationServer.getReceiveBufferSize());
    }

    @Override
    public void run() {
        running = true;

        long start;
        long end;
        while(running) {
            if (Logger.BENCHMARK_REPLICATION)
                start = System.nanoTime();

            Client client = database.poll(server);
            client.setMode(Request.Mode.REPLICATION);
            DatagramPacket packet = client.getPacket();

            if (Logger.BENCHMARK_REPLICATION) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_REPLICATION, start, end, "client");
                start = System.nanoTime();
            }

            try {
                replicationServer.receive(packet);

                if (Logger.BENCHMARK_REPLICATION) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_REPLICATION, start, end, "receive");
                    start = System.nanoTime();
                }

                executor.execute(client);

                if (Logger.BENCHMARK_REPLICATION) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_REPLICATION, start, end, "start client");
                }
            } catch (OutOfMemoryError e) {
                Reply.setReply(client.getPacket(), Protocol.ERROR_MEMORY);

                try {
                    replicationServer.send(client.getPacket());
                } catch (IOException ioe) {
                    if (Logger.VERBOSE_REPLICATION)
                        Logger.log(Logger.TAG_REPLICATION, ioe);
                }
            } catch (IOException e) {
                if (Logger.VERBOSE_REPLICATION)
                    Logger.log(Logger.TAG_REPLICATION, e);
            }
        }

        if (Logger.VERBOSE_REPLICATION)
            Logger.log(Logger.TAG_REPLICATION, "stopped");
    }

    public void exit() {
        running = false;
        replicationServer.close();
    }

    public void send(DatagramPacket packet, Node node) throws IOException {
        packet.setAddress(node.getIp());
        packet.setPort(node.getPort() + Protocol.PORT_REPLICATION);

        if (Logger.VERBOSE_REPLICATION)
            Logger.log(Logger.TAG_REPLICATION, "sending to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort() + "]: " + packet.getLength());

        long start;
        if (Logger.BENCHMARK_REPLICATION)
            start = System.nanoTime();

        replicationServer.send(packet);

        if (Logger.BENCHMARK_REPLICATION) {
            long end = System.nanoTime();
            Logger.benchmark(Logger.TAG_REPLICATION, start, end, "send");
        }
    }
}
