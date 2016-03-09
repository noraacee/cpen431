package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Reply;
import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.protocol.Retry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server extends Protocol {
    private static final int SIZE_POOL = 24;

    private AtomicBoolean shutdown;

    private Database database;
    private DatagramSocket server;
    private ExecutorService executor;


    public Server(InetAddress ip, int port) throws SocketException {
        shutdown = new AtomicBoolean(false);

        database = new Database();
        server = new DatagramSocket(port, ip);

        executor = Executors.newFixedThreadPool(SIZE_POOL);
    }

    /**
     * Accepts requests from client
     */
    public void accept() {
        initRetryThread();

        while(!shutdown.get()) {
            DatagramPacket packet = null;
            try {
                byte[] data = new byte[LENGTH_TOTAL];
                packet = new DatagramPacket(data, LENGTH_TOTAL);

                server.receive(packet);

                Request request = new Request(shutdown, database, packet);

                executor.execute(new Client(server, request));
            } catch (OutOfMemoryError e) {
                if (packet != null) {
                    Reply.setReply(packet, ERROR_MEMORY);
                    try {
                        server.send(packet);
                    } catch (IOException ioe) {
                        System.out.println(ERROR_MESSAGE_SEND + e.getMessage());
                    }
                }
            } catch (Exception e) {
                if (packet != null) {
                    Reply.setReply(packet, ERROR_FAILURE);
                    try {
                        server.send(packet);
                    } catch (IOException ioe) {
                        System.out.println(ERROR_MESSAGE_SEND + e.getMessage());
                    }
                }

                System.out.println(ERROR_MESSAGE_RECEIVE + e.getMessage());
            }
        }

        executor.shutdown();
    }

    /**
     * Initialize the thread that manages retries
     */
    private void initRetryThread() {
        new Thread(new Retry(shutdown, database)).start();
    }
}
