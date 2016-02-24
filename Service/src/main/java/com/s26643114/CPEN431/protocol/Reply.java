package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.model.NodeBase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Reply implements Runnable {
    private ConcurrentLinkedQueue<NodeBase> nodes;
    private PrintWriter writer;
    private Socket client;

    public Reply(Socket client, ConcurrentLinkedQueue<NodeBase> nodes, PrintWriter writer) {
        this.client = client;
        this.nodes = nodes;
        this.writer = writer;
    }

    public void run() {
        NodeBase nodeBase = null;
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            System.out.println("Reading");

            String node = in.readLine();

            System.out.println("Read: " + node);

            JSONObject obj = (JSONObject) new JSONParser().parse(node);
            if (obj.get("login") == "Yes") {
                nodeBase = nodes.poll();
                if (nodeBase != null) {
                    String msg = nodeBase.getHostName() + "-" + nodeBase.getNodeId();

                    System.out.println("Printing: " + msg);

                    out.println(msg);

                    System.out.print("Printed");
                } else {
                    out.println(0);
                }
            }

            out.flush();
            out.close();
            client.close();

            synchronized (writer) {
                writer.write(node);
            }
        } catch (Exception e) {
            if (nodeBase != null)
                nodes.add(nodeBase);
        }
    }
}
