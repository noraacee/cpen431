package com.s26643114.CPEN431.application;

import com.s26643114.CPEN431.model.NodeBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatusMonitoringService {
    private static final String PATH_NODES = "nodes.txt";
    private static final String REGEX_NODE = ":";

    List<NodeBase> nodes;

    public static void main(String[] args) throws IOException {

    }

    public void parseNodesList() throws IOException {
        nodes = new ArrayList<>();

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
