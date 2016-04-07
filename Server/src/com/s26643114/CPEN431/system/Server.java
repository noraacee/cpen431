package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Node;
import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Reply;
import com.s26643114.CPEN431.protocol.Retry;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private volatile boolean running;

    private Database database;
    private DatagramSocket server;
    private ExecutorService executor;

    private HeartbeatServer heartbeatServer;
    private InternalServer internalServer;
    private Retry retry;


    public Server(InetAddress ip, int port) throws IOException {
        running = false;

        database = new Database();
        server = new DatagramSocket(port, ip);
        server.setReceiveBufferSize(Protocol.SIZE_BUFFER);

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "receive buffer: " + server.getReceiveBufferSize());

        executor = Executors.newCachedThreadPool();

        heartbeatServer = new HeartbeatServer(ip, port);
        internalServer = new InternalServer(this, executor, database, ip, port);
        retry = new Retry(database);

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "started");
    }

    /**
     * Accepts requests from client
     */
    public void accept() {
        running = true;

        internalServer.start();
        retry.start();

        long start;
        long end;
        while(running) {
            if (Logger.BENCHMARK_SERVER)
                start = System.nanoTime();

            Client client = database.poll(this);
            DatagramPacket packet = client.getPacket();

            if (Logger.BENCHMARK_SERVER) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_SERVER, start, end, "client");
                start = System.nanoTime();
            }

            try {
                server.receive(packet);

                if (Logger.BENCHMARK_SERVER) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_SERVER, start, end, "receive");
                    start = System.nanoTime();
                }

                heartbeatServer.start();

                executor.execute(client);

                if (Logger.BENCHMARK_SERVER) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_SERVER, start, end, "start client");
                }
            } catch (OutOfMemoryError e) {
                Reply.setReply(client.getPacket(), Protocol.ERROR_MEMORY);

                try {
                    server.send(client.getPacket());
                } catch (IOException ioe) {
                    if (Logger.VERBOSE_SERVER)
                        Logger.log(Logger.TAG_SERVER, ioe);
                }
            } catch (IOException e) {
                if (Logger.VERBOSE_SERVER)
                    Logger.log(Logger.TAG_SERVER, e);
            }
        }

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "stopped");
    }

    public void exit() {
        running = false;

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down threads");
        executor.shutdown();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down retry thread");
        retry.exit();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down heartbeat server");
        heartbeatServer.exit();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down internal server");
        internalServer.exit();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down socket");
        server.close();
    }

    /**
     * Sends a packet using server socket
     */
    public void send(DatagramPacket packet) throws IOException {
        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "sending to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort() + "]: " + packet.getLength());

        long start;
        if (Logger.BENCHMARK_SERVER)
            start = System.nanoTime();

        server.send(packet);

        if (Logger.BENCHMARK_SERVER) {
            long end = System.nanoTime();
            Logger.benchmark(Logger.TAG_SERVER, start, end, "send");
        }
    }

    public void sendInternal(DatagramPacket packet, boolean replicate, Node[] replicas) throws IOException {
        internalServer.send(packet, replicate, replicas);
    }

    public void sendReplicas(DatagramPacket packet, Node[] replicas) throws IOException {
        internalServer.sendReplicas(packet, replicas);
    }
}
