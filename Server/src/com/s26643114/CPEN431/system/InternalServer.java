package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Route;
import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternalServer extends Thread {
    private long end;
    private long start;

    private AtomicBoolean shutdown;
    private Database database;
    private DatagramSocket internalServer;

    public InternalServer(AtomicBoolean shutdown, Database database) throws SocketException {
        this.shutdown = shutdown;
        this.database = database;
        internalServer = new DatagramSocket();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "started");
    }

    @Override
    public void interrupt() {
        super.interrupt();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "shutting down internal server socket");

        internalServer.close();

        if (Logger.VERBOSE_INTERNAL)
            Logger.log(Logger.TAG_INTERNAL, "stopped");
    }

    @Override
    public void run() {
        DatagramPacket packet = new DatagramPacket(new byte[Protocol.LENGTH_INSTANT], Protocol.LENGTH_INSTANT);
        AtomicBoolean lock;
        while(!shutdown.get()) {
            try {
                if (Logger.BENCHMARK_INTERNAL)
                    start = System.nanoTime();

                internalServer.receive(packet);

                if (Logger.BENCHMARK_INTERNAL) {
                    end = System.nanoTime();
                    Logger.benchmark(Logger.TAG_INTERNAL, start, end, "receive");
                }

                long lockKey = Route.convertKey(packet);

                if (Logger.VERBOSE_INTERNAL)
                    Logger.log(Logger.TAG_INTERNAL, "acknowledgement received from [" + packet.getAddress().getHostAddress() + ":"
                            + packet.getPort() + "] with key: " + lockKey);

                synchronized (lock = database.remove(lockKey)) {
                    lock.notify();
                }
            } catch (IOException e) {
                if (Logger.VERBOSE_INTERNAL)
                    Logger.log(Logger.TAG_INTERNAL, e);

                internalServer.close();
                shutdown.set(true);
                break;
            } catch (NullPointerException ignored) {
            }
        }
    }

    public void send(DatagramPacket packet, long key, AtomicBoolean lock) throws IOException {
        database.put(key, lock);

        if (Logger.BENCHMARK_INTERNAL)
            start = System.nanoTime();

        internalServer.send(packet);

        if (Logger.BENCHMARK_INTERNAL) {
            end = System.nanoTime();
            Logger.benchmark(Logger.TAG_INTERNAL, start, end, "send");
        }
    }
}
