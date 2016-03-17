package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Reply;
import com.s26643114.CPEN431.protocol.Retry;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server extends Protocol {
    private long end;
    private long start;

    private AtomicBoolean shutdown;

    private Database database;
    private DatagramSocket server;
    private ExecutorService executor;

    private InternalServer internalServer;
    private Retry retry;


    public Server(InetAddress ip, int port) throws SocketException {
        shutdown = new AtomicBoolean(false);

        database = new Database();
        server = new DatagramSocket(port, ip);

        executor = Executors.newFixedThreadPool(SIZE_POOL_THREAD);

        internalServer = new InternalServer(shutdown, database);
        retry = new Retry(shutdown, database);

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "started");
    }

    /**
     * Accepts requests from client
     */
    public void accept() {
        internalServer.start();
        retry.start();

        while(!shutdown.get()) {
            Client client = database.pop(this, shutdown);
            try {
                if (Logger.BENCHMARK_SERVER)
                    start = System.nanoTime();

                server.receive(client.getPacket());

                if (Logger.BENCHMARK_SERVER) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_SERVER, start, end, "receive");
                    start = System.nanoTime();
                }

                executor.execute(client);

                if (Logger.BENCHMARK_SERVER) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_SERVER, start, end, "start client");
                }
            } catch (OutOfMemoryError e) {
                Reply.setReply(client.getPacket(), ERROR_MEMORY);

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
            Logger.log(Logger.TAG_SERVER, "shutting down threads");
        executor.shutdown();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down retry thread");
        retry.interrupt();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down internal server");
        internalServer.interrupt();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "shutting down socket");
        server.close();

        if (Logger.VERBOSE_SERVER)
            Logger.log(Logger.TAG_SERVER, "stopped");
    }

    public String getAddress() {
        return server.getLocalAddress().getHostAddress() + ":" + Integer.toString(server.getLocalPort());
    }

    public void removeInternal(long key) {
        database.remove(key);
    }

    /**
     * Sends a packet using server socket
     */
    public void send(DatagramPacket packet) throws IOException {
        if (Logger.BENCHMARK_SERVER)
            start = System.nanoTime();

        server.send(packet);

        if (Logger.BENCHMARK_SERVER) {
            end = System.nanoTime();
            Logger.benchmark(Logger.TAG_SERVER, start, end, "send");
        }
    }

    public AtomicBoolean sendInternal(DatagramPacket packet, long key, AtomicBoolean lock) throws IOException {
        internalServer.send(packet, key, lock);
        return lock;
    }
}
