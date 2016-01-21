package com.s26643114.CPEN431.application;

import com.s26643114.CPEN431.protocol.Request;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 13114;

    private static final String ERROR_NUMBER_FORMAT = "Second, third and fourth argument can only be integers";
    private static final String ERROR_SOCKET = "An error has occurred while creating the socket";
    private static final String ERROR_UNKNOWN_HOST = "Cannot determine ip host";

    private static final String IP = "ssh-linux4.ece.ubc.ca";

    public static void main(String[] args) {
        InetAddress ip;
        int port;

        try {
            switch (args.length) {
                case 1:
                    ip = InetAddress.getByName(IP);
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
        } catch (NumberFormatException e) {
            System.out.println(ERROR_NUMBER_FORMAT);
            return;
        } catch (UnknownHostException e) {
            System.out.println(ERROR_UNKNOWN_HOST);
            return;
        }

        DatagramSocket server;
        try {
            server = new DatagramSocket(port, ip);
        } catch (SocketException e) {
            System.out.println(ERROR_SOCKET);
            return;
        }

        ConcurrentHashMap<byte[], byte[]> store = new ConcurrentHashMap<>();

        Request request = new Request(server, store);
        request.receive();
    }
}
