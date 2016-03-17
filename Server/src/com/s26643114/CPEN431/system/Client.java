package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles interaction with a client
 */
public class Client extends Request implements Runnable {
    private long end;
    private long start;

    private Server server;

    public Client(Server server, AtomicBoolean shutdown, Database database) {
        super(shutdown, database);
        this.server = server;
    }

    @Override
    public void run() {
        try {
            if (Logger.BENCHMARK_CLIENT)
                start = System.nanoTime();

            DatagramPacket reply = parse();

            if (Logger.BENCHMARK_CLIENT) {
                end = System.nanoTime();
                Logger.benchmark(Logger.TAG_CLIENT, start, end, "request parse");
            }

            if (reply == null)
                return;

            try {
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

        database.push(this);
    }

    public void reject(byte errorCode) {
        try {
            server.send(createReject(errorCode));
        } catch (IOException e) {
            if (Logger.VERBOSE_CLIENT)
                Logger.log(Logger.TAG_CLIENT, e);
        }
    }
}
