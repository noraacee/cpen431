package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.ut
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles request side of the protocol
 */
public class Request implements Runnable {
    private byte[] uniqueId;

    private ConcurrentHashMap<byte[], Integer> queue;
    private DatagramSocket client;

    public Request(ConcurrentHashMap<byte[], Integer> queue, DatagramSocket client, byte[] uniqueId, DatagramPacket sendPacket) {
        this.queue = queue;
        this.client = client;
    }

    @Override
    public void run() {
        queue
    }

    /**
     * Sends a request to the server
     *
     * @return reply from the server
     * @throws IOException - exception from using the socket
     */
    public byte[] sendRequest() throws IOException {
        DatagramPacket sendPacket = Protocol.createSendPacket(ip, port, uniqueId, payload);

        byte[] reply = new byte[Protocol.LENGTH_PAYLOAD_REPLY];
        DatagramPacket receivePacket = new DatagramPacket(reply, reply.length);

        int retries = Protocol.RETRIES;
        while (retries >= 0) {
            client.setSoTimeout(timeout);
            client.send(sendPacket);
            try {
                while(true) {
                    client.receive(receivePacket);

                    retries = Protocol.RETRIES; //resets retries because reply received but not might be correct unique id

                    if (Protocol.checkUniqueId(uniqueId, receivePacket.getData()))
                        break;
                }

                return receivePacket.getData();
            } catch (SocketTimeoutException e) {
                timeout *= 2;
                retries --;
            }
        }

        throw new SocketTimeoutException();
    }
}
