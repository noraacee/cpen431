package com.s26643114.CPEN431.application;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class StatusMonitoringService {
    private static final String PATH_NODES = "node.txt";

    public static void main(String[] args) throws FileNotFoundException {
        FileReader reader = new FileReader(PATH_NODES);
    }
}
