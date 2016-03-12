package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Request;
import com.s26643114.CPEN431.util.Logger;

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
                if (Logger.VERBOSE_CLIENT)
                    Logger.log(e);
            }
        } catch (OutOfMemoryError e) {
            reject(ERROR_MEMORY);
            if (Logger.VERBOSE_CLIENT)
                Logger.log(e);
        } catch (Exception e) {
            reject(ERROR_FAILURE);
            if (Logger.VERBOSE_CLIENT)
                Logger.log(e);
        }
    }

    public void reject(byte errorCode) {
        try {
            server.send(request.reject(errorCode));
        } catch (IOException e) {
            if (Logger.VERBOSE_CLIENT)
                Logger.log(e);
        }
    }
}
