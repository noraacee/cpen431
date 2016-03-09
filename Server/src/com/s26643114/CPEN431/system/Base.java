package com.s26643114.CPEN431.system;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Base with key-value store service
 */
public class Base {
    private static final int PORT = 12664;

    private static final String ERROR_NUMBER_FORMAT = "Port must be an integer";
    private static final String ERROR_SOCKET = "An error has occurred while creating the socket. System returned with error: ";
    private static final String ERROR_UNKNOWN_HOST = "Cannot determine ip host";

    public static void main(String[] args) {
        InetAddress ip;
        int port;

        try {
            switch (args.length) {
                case 1:
                    ip = InetAddress.getLocalHost();
                    port = Integer.parseInt(args[0]);
                    break;
                case 2:
                    ip = InetAddress.getByName(args[0]);
                    port = Integer.parseInt(args[1]);
                    break;
                default:
                    ip = InetAddress.getLocalHost();
                    port = PORT;
                    break;
            }

            System.out.println(ip.getHostAddress() + ":" + port);
        } catch (NumberFormatException e) {
            System.out.println(ERROR_NUMBER_FORMAT);
            return;
        } catch (UnknownHostException e) {
            System.out.println(ERROR_UNKNOWN_HOST);
            return;
        }

        try {
            Server server = new Server(ip, port);
            server.accept();
        } catch (SocketException e) {
            System.out.println(ERROR_SOCKET + e.getMessage());
        }
    }
}
