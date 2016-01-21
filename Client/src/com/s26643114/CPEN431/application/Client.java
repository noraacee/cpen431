package com.s26643114.CPEN431.application;

import com.s26643114.CPEN431.protocol.Protocol;
import com.s26643114.CPEN431.protocol.Reply;
import com.s26643114.CPEN431.protocol.Request;

import java.io.IOException;
import java.net.*;

/**
 * Reads input from client to determine student number to send and timeout duration. Displays reply from server.
 */
public class Client {
    private static final int PORT = 5627;

    private static final String ERROR_ARGS = "You must supply at least the student number";
    private static final String ERROR_IO = "An error has occurred when sending request";
    private static final String ERROR_IP = "An error has occurred when retrieving an ip address";
    private static final String ERROR_NUMBER_FORMAT = "Second, third and fourth argument can only be integers";
    private static final String ERROR_SOCKET = "An error has occurred while creating the socket";
    private static final String ERROR_TIMEOUT = "The request has been timed out. Please try again with a longer timeout duration.";

    private static final String IP = "162.219.6.226";

    public static void main(String args[]) {
        try {
            if (args.length == 0) {
                System.out.println(ERROR_ARGS);
                return;
            }

            InetAddress ip;
            int port;
            int payload;
            int timeout = Protocol.TIMEOUT_SOCKET;

            try {
                switch (args.length) {
                    case 2:
                        timeout = Integer.parseInt(args[1]);
                    case 1:
                        ip = InetAddress.getByName(IP);
                        port = PORT;
                        payload = Integer.parseInt(args[0]);
                        break;
                    case 4:
                        timeout = Integer.parseInt(args[3]);
                    case 3:
                        ip = InetAddress.getByName(IP);
                        port = Integer.parseInt(args[1]);
                        payload = Integer.parseInt(args[2]);
                        break;
                    default:
                        System.out.println(ERROR_ARGS);
                        return;
                }
            } catch (NumberFormatException e) {
                System.out.println(ERROR_NUMBER_FORMAT);
                return;
            }

            DatagramSocket client = new DatagramSocket();
            client.setSoTimeout(timeout);

            byte[] uniqueId = Protocol.createUniqueId(InetAddress.getLocalHost(), client.getPort());
            try {
                System.out.println("Sending ID: " + payload);

                byte[] reply = Request.sendRequest(client, ip, port, uniqueId, payload, timeout);

                System.out.println("Secret Code: " + Reply.parseReply(reply));
            } catch (SocketTimeoutException e) {
                System.out.println(ERROR_TIMEOUT);
            } catch (IOException e) {
                System.out.println(ERROR_IO);
            }

            client.close();
        } catch (SocketException e) {
            System.out.println(ERROR_SOCKET);
        } catch (UnknownHostException e) {
            System.out.println(ERROR_IP);
        }
    }
}
