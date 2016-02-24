package com.s26643114.CPEN431.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Report {
    private Socket client;

    public Report(String ip, String port) throws IOException {
        System.out.println("Connecting socket:" + ip + ":" + port);
        client = new Socket(ip, Integer.parseInt(port));
        System.out.println("Socket connected");
    }

    public String reportNode(String node) {
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            System.out.println("Printing: " + node);
            out.println(node);

            String nextNode = in.readLine();

            System.out.println("Received: " + nextNode);

            out.flush();
            out.close();
            client.close();

            return nextNode;
        } catch (IOException e) {
            return null;
        }
    }
}
