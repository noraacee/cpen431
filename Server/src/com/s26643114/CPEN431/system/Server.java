package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Reply;
import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.protocol.Retry;
//import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server extends Protocol {
    private static final int SIZE_POOL = 32;

    //private static final String TAG = "server";

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

        executor = Executors.newFixedThreadPool(SIZE_POOL);

        internalServer = new InternalServer(shutdown, database);
        retry = new Retry(shutdown, database);
    }

    /**
     * Accepts requests from client
     */
    public void accept() {
        internalServer.start();
        retry.start();

        while(!shutdown.get()) {
            DatagramPacket packet = null;
            try {
                packet = new DatagramPacket(new byte[LENGTH_TOTAL], LENGTH_TOTAL);

                server.receive(packet);

                Request request = new Request(shutdown, database, packet);

                executor.execute(new Client(this, request));
            } catch (OutOfMemoryError e) {
                if (packet != null) {
                    Reply.setReply(packet, ERROR_MEMORY);
                    try {
                        server.send(packet);
                    } catch (IOException ioe) {
                        //if (Logger.VERBOSE_SERVER)
                            //Logger.log(TAG, ioe);
                    }
                }
            } catch (Exception e) {
                if (packet != null) {
                    Reply.setReply(packet, ERROR_FAILURE);
                    try {
                        server.send(packet);
                    } catch (IOException ioe) {
                        //if (Logger.VERBOSE_SERVER)
                            //Logger.log(TAG, ioe);
                    }
                }

                //if (Logger.VERBOSE_SERVER)
                    //Logger.log(TAG, e);
            }
        }

        //if (Logger.VERBOSE_SERVER)
            //Logger.log(TAG, "shutting down threads");
        executor.shutdown();

        //if (Logger.VERBOSE_SERVER)
            //Logger.log(TAG, "shutting down retry thread");
        retry.interrupt();

        //if (Logger.VERBOSE_SERVER)
            //Logger.log(TAG, "shutting down internal server");
        internalServer.interrupt();

        //if (Logger.VERBOSE_SERVER)
            //Logger.log(TAG, "shutting down socket");
        server.close();
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
        server.send(packet);
    }

    public AtomicBoolean sendInternal(DatagramPacket packet, long key, AtomicBoolean lock) throws IOException {
        internalServer.send(packet, key, lock);
        return lock;
    }
}
