package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.model.NodeBase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Request implements Runnable {
    private ConcurrentLinkedQueue<NodeBase> nodes;
    private PrintWriter writer;

    public Request(ConcurrentLinkedQueue<NodeBase> nodes) throws FileNotFoundException, UnsupportedEncodingException {
        this.nodes = nodes;
        writer = new PrintWriter("data.json", "UTF-8");
    }

    public void run() {
        try {
            ServerSocket server = new ServerSocket(0);
            System.out.println(InetAddress.getLocalHost() + ":" + server.getLocalPort());
            while(true) {
                new Thread(new Reply(server.accept(), nodes, writer)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
