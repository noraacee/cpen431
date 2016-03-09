package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.model.NodeBase;
import com.s26643114.CPEN431.protocol.Request;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Service {
    private static final String PATH_NODES = "nodes.txt";
    private static final String REGEX_NODE = ":";

    private static ConcurrentLinkedQueue<NodeBase> nodes;

    public static void main(String[] args) throws IOException {
        //parseNodesList();
        nodes = new ConcurrentLinkedQueue<NodeBase>();
        NodeBase nodeBase1 = new NodeBase("planetlab2.cs.ubc.ca", "15");

        nodes.add(nodeBase1);

        new Thread(new Request(nodes)).start();
    }

    private static void parseNodesList() throws IOException {
        nodes = new ConcurrentLinkedQueue<NodeBase>();

        FileReader reader = new FileReader(PATH_NODES);
        BufferedReader bReader = new BufferedReader(reader);

        String line;
        while((line = bReader.readLine()) != null) {
            String[] nodeInfo = line.split(REGEX_NODE);
            nodes.add(new NodeBase(nodeInfo[0], nodeInfo[1]));
        }

        bReader.close();
    }
}
