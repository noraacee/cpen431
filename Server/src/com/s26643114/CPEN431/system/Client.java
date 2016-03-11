package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.util.Logging;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Handles interaction with a client
 */
public class Client extends Protocol implements Runnable {
    private Server server;
    private Request request;

    public Client(Server server, Request request) {
        this.server = server;
        this.request = request;
    }

    @Override
    public void run() {
        try {
            DatagramPacket reply = request.parse();
            if (reply == null)
                return;

            try {
                server.send(reply);
                request.cache();
            } catch (IOException e) {
                if (Logging.VERBOSE_CLIENT)
                    Logging.log(e);
            }
        } catch (OutOfMemoryError e) {
            reject(ERROR_MEMORY);
            if (Logging.VERBOSE_CLIENT)
                Logging.log(e);
        } catch (Exception e) {
            reject(ERROR_FAILURE);
            if (Logging.VERBOSE_CLIENT)
                Logging.log(e);
        }
    }

    public void reject(byte errorCode) {
        try {
            server.send(request.reject(errorCode));
        } catch (IOException e) {
            if (Logging.VERBOSE_CLIENT)
                Logging.log(e);
        }
    }
}
