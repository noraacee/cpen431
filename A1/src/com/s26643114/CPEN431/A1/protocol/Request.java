package com.s26643114.CPEN431.A1.protocol;


import com.s26643114.CPEN431.A1.util.ByteOrder;
import com.s26643114.CPEN431.A1.util.StringUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Random;

/**
 * Handles request side of the protocol
 */
public class Request {
    public static final int TIMEOUT_SOCKET = 100;

    private static final int LENGTH_IP = 4;
    private static final int LENGTH_PAYLOAD = 4;
    private static final int LENGTH_PORT = 2;
    private static final int LENGTH_RANDOM_BYTES = 2;
    private static final int LENGTH_TIME = 8;
    private static final int LENGTH_UNIQUE_ID = 16;

    private static final int RETRIES = 3;

    private static final String ERROR_TIMEOUT = "Retry ";

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
     * Sends a request to the server
     *
     * @param client - socket used for UDP protocol
     * @param ip - ip of server
     * @param port - port that server is listening to
     * @param uniqueId - unique id generated previously
     * @param payload - student number
     * @param timeout - timeout duration
     * @return reply from the server
     * @throws IOException - exception from using the socket
     */
    public static byte[] sendRequest(DatagramSocket client, InetAddress ip, int port, byte[] uniqueId, int payload, int timeout) throws IOException {
        DatagramPacket sendPacket = createSendPacket(ip, port, uniqueId, payload);

        byte[] reply = new byte[Reply.LENGTH_PAYLOAD];
        DatagramPacket receivePacket = new DatagramPacket(reply, reply.length);

        int retries = RETRIES;
        while (retries >= 0) {
            client.setSoTimeout(timeout);
            client.send(sendPacket);
            try {
                while(true) {
                    client.receive(receivePacket);

                    retries = RETRIES; //resets retries because reply received but not might be correct unique id

                    //System.out.println("Reply: " + StringUtils.byteArrayToHexString(receivePacket.getData()));

                    if (checkUniqueId(uniqueId, receivePacket.getData()))
                        break;
                }

                return receivePacket.getData();
            } catch (SocketTimeoutException e) {
                timeout *= 2;
                retries --;

                //System.out.println(ERROR_TIMEOUT + (RETRIES - retries));
            }
        }

        throw new SocketTimeoutException();
    }

    /**
     * Compare the unique id used in the request with the reply
     *
     * @param uniqueId - unique id used in request
     * @param reply - reply from
     * @return if the unique id used in the request matches the unique id in the reply
     */
    private static boolean checkUniqueId(byte[] uniqueId, byte[] reply) {
        byte[] uniqueIdCheck = new byte[uniqueId.length];
        System.arraycopy(reply, 0, uniqueIdCheck, 0, uniqueId.length);

        return Arrays.equals(uniqueId, uniqueIdCheck);
    }

    /**
     * Creates a send packet to use in the request
     *
     * @param ip - ip of server
     * @param port - port that server is listening to
     * @param uniqueId - unique id generated previously
     * @param payload - student number
     * @return request packet to send to server
     */
    private static DatagramPacket createSendPacket(InetAddress ip, int port, byte[] uniqueId, int payload) {
        byte[] request = new byte[LENGTH_UNIQUE_ID + LENGTH_PAYLOAD];

        byte[] payloadBytes = new byte[LENGTH_PAYLOAD];
        ByteOrder.int2leb(payload, payloadBytes, 0);
        payloadBytes = ByteOrder.reverse(payloadBytes);

        System.arraycopy(uniqueId, 0, request, 0, LENGTH_UNIQUE_ID);
        System.arraycopy(payloadBytes, 0, request, LENGTH_UNIQUE_ID, LENGTH_PAYLOAD);

        //System.out.println("Request: " + StringUtils.byteArrayToHexString(request));

        return new DatagramPacket(request, request.length, ip, port);
    }
}
