package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Request;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Handles interaction with a client
 */
public class Client extends Protocol implements Runnable {
    private DatagramSocket server;
    private Request request;

    public Client(DatagramSocket server, Request request) {
        this.server = server;
        this.request = request;
    }

    @Override
    public void run() {
        try {
            DatagramPacket reply = request.parse();
            try {
                server.send(reply);
                request.cache();
            } catch (IOException e) {
                System.out.println(ERROR_MESSAGE_SEND + e.getMessage());
            }
        } catch (OutOfMemoryError e) {
            reject(ERROR_MEMORY);
        } catch (Exception e) {
            reject(ERROR_FAILURE);
        }
    }

    public void reject(byte errorCode) {
        try {
            server.send(request.reject(errorCode));
        } catch (IOException e) {
            System.out.println(ERROR_MESSAGE_SEND + e.getMessage());
        }
    }
}
