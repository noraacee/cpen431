package com.s26643114.CPEN431.protocol;


import com.s26643114.CPEN431.util.ByteOrder;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

public class Protocol {
    protected static final int LENGTH_IP = 4;
    protected static final int LENGTH_PAYLOAD_REQUEST = 4;
    protected static final int LENGTH_PAYLOAD_REPLY = 16000;
    protected static final int LENGTH_PORT = 2;
    protected static final int LENGTH_RANDOM_BYTES = 2;
    protected static final int LENGTH_TIME = 8;
    protected static final int LENGTH_UNIQUE_ID = 16;

    protected static final int RETRIES = 3;
    public static final int TIMEOUT_SOCKET = 100;

    /**
     * Creates a unique id to use in a request
     *
     * @param ip - ip of client
     * @param port - port to listen for server reply
     * @return byte array of unique id
     */
    public static byte[] createUniqueId(InetAddress ip, int port) {
        byte[] uniqueId = new byte[LENGTH_UNIQUE_ID];

        int index = 0;

        byte[] ipBytes = ip.getAddress();
        System.arraycopy(ipBytes, 0, uniqueId, index, LENGTH_IP);

        index += LENGTH_IP;

        for (int i = 0; i < LENGTH_PORT; i++) {
            uniqueId[index + i] = (byte) (port & 0xFF);
            port >>= Byte.SIZE;
        }

        index += LENGTH_PORT;

        byte[] randomBytes = new byte[LENGTH_RANDOM_BYTES];
        new Random().nextBytes(randomBytes);
        System.arraycopy(randomBytes, 0, uniqueId, index, LENGTH_RANDOM_BYTES);

        index += LENGTH_RANDOM_BYTES;

        long time = System.currentTimeMillis();
        for (int i = 0; i < LENGTH_TIME; i++) {
            uniqueId[index + i] = (byte) (time & 0xFF);
            time >>= Byte.SIZE;
        }

        return uniqueId;
    }

    /**
     * Compare the unique id used in the request with the reply
     *
     * @param uniqueId - unique id used in request
     * @param reply - reply from
     * @return if the unique id used in the request matches the unique id in the reply
     */
    public static boolean checkUniqueId(byte[] uniqueId, byte[] reply) {
        byte[] uniqueIdCheck = new byte[uniqueId.length];
        System.arraycopy(reply, 0, uniqueIdCheck, 0, uniqueId.length);

        return Arrays.equals(uniqueId, uniqueIdCheck);
    }

    /**
     * Creates a send packet to use in the request
     *
     * @return request packet to send to server
     */
    public static DatagramPacket createSendPacket(InetAddress ip, int port, byte[] uniqueId, int payload) {
        byte[] request = new byte[Protocol.LENGTH_UNIQUE_ID + Protocol.LENGTH_PAYLOAD_REQUEST];

        byte[] payloadBytes = new byte[Protocol.LENGTH_PAYLOAD_REQUEST];
        ByteOrder.int2leb(payload, payloadBytes, 0);
        payloadBytes = ByteOrder.reverse(payloadBytes);

        System.arraycopy(uniqueId, 0, request, 0, Protocol.LENGTH_UNIQUE_ID);
        System.arraycopy(payloadBytes, 0, request, Protocol.LENGTH_UNIQUE_ID, Protocol.LENGTH_PAYLOAD_REQUEST);

        return new DatagramPacket(request, request.length, ip, port);
    }
}
