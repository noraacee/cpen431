package com.s26643114.CPEN431.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Test {
    public static final int LENGTH_CODE = 1;
    public static final int LENGTH_INSTANT = Long.BYTES;
    public static final int LENGTH_IP = 4;
    public static final int LENGTH_KEY = 32;
    public static final int LENGTH_PORT = 2;
    public static final int LENGTH_UNIQUE_ID = 16;
    public static final int LENGTH_VALUE = 10000;
    public static final int LENGTH_VALUE_LENGTH = 2;
    public static final int LENGTH_TOTAL = LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH
            + LENGTH_VALUE + LENGTH_IP + LENGTH_PORT + LENGTH_INSTANT;

    private static final int PORT_SERVER = 12668;
    private static final int RETRIES = 3;
    private static final int TIMEOUT = 5000;

    private static final String DELIMITER_NODE = ":";
    private static final String FILENAME_NODES = "nodes.list";
    private static final String IGNORED_NODE = "#";

    public static void main (String args[]) throws IOException {
        FileReader fileReader = new FileReader(FILENAME_NODES);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        List<InetAddress> nodes = new ArrayList<>();
        String line;
        String[] parts;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains(IGNORED_NODE))
                continue;

            if (!line.contains(DELIMITER_NODE))
                continue;

            parts = line.trim().split(DELIMITER_NODE);

            nodes.add(InetAddress.getByName(parts[0]));
        }
    }

    private class ClientThread implements Runnable {
        private AtomicBoolean success;
        private DatagramPacket packet;
        private DatagramSocket socket;

        public ClientThread(AtomicBoolean success, InetAddress ip) throws IOException {
            this.success = success;

            packet = new DatagramPacket(new byte[LENGTH_TOTAL], LENGTH_TOTAL, ip, PORT_SERVER);
            socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT);
        }

        @Override
        public void run() {
            try {
                success.set(true);
                for (int i = 0; i < RETRIES; i++) {
                    packet.getData()[0] = (byte) i;
                    socket.send(packet);

                    int retries = RETRIES;
                    while (retries > 0) {
                        try {
                            socket.receive(packet);
                            break;
                        } catch (SocketTimeoutException e) {
                            retries--;
                        }
                    }

                    if (packet.getData()[0] != (byte) i) {
                        success.set(false);
                        break;
                    }
                }
            } catch (IOException e) {
                success.set(false);
            }
        }
    }

    private class ServerThread implements Runnable {
        private DatagramPacket packet;
        private DatagramSocket socket;

        @Override
        public void run() {
            try {
                packet = new DatagramPacket(new byte[LENGTH_TOTAL], LENGTH_TOTAL);
                socket = new DatagramSocket(PORT_SERVER);

                while(true) {
                    socket.receive(packet);
                    socket.send(packet);
                }
            } catch (IOException ignored) {}
        }
    }
}
