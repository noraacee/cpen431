package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Handles interaction with a client
 */
public class Client extends Request implements Runnable {
    private long end;
    private long start;

    private Server server;

    public Client(Server server, Database database) {
        super(database);
        this.server = server;
    }

    /**
     * Parses a packet and replies to the sender
     */
    @Override
    public void run() {
        try {
            if (Logger.BENCHMARK_CLIENT)
                start = System.nanoTime();

            if (init()) {
                DatagramPacket packet = getPacket();
                InetAddress ip = packet.getAddress();
                int port = packet.getPort();

                server.sendReplicas(packet, replicas);

                packet.setAddress(ip);
                packet.setPort(port);
            }

            DatagramPacket reply = parse();

            if (Logger.BENCHMARK_CLIENT) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_CLIENT, start, end, "request parse");
            }

            try {
                if (mode == Mode.INTERNAL)
                    server.sendInternal(reply, replicate, replicas);
                else if (mode == Mode.EXTERNAL)
                    server.send(reply);
            } catch (IOException e) {
                if (Logger.VERBOSE_CLIENT)
                    Logger.log(Logger.TAG_CLIENT, e);
            }
        } catch (OutOfMemoryError e) {
            reject(ERROR_MEMORY);
        } catch (Exception e) {
            reject(ERROR_FAILURE);

            if (Logger.VERBOSE_CLIENT)
                Logger.log(Logger.TAG_CLIENT, e);
        }

        database.offer(this);

        if (shutdown)
            server.exit();
    }

    /**
     * Rejects a packet with error code
     */
    public void reject(byte errorCode) {
        try {
            server.send(createReject(errorCode));
        } catch (IOException e) {
            if (Logger.VERBOSE_CLIENT)
                Logger.log(Logger.TAG_CLIENT, e);
        }
    }
}
