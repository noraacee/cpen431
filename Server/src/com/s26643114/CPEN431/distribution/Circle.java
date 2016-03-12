package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.util.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class Circle {
    private static final String ALGORITHM_HASH = "MD5";
    private static final String DELIMITER_NODE = ":";
    private static final String IGNORED_NODE = "#";

    private SortedMap<BigInteger, Node> nodes;

    public Circle(String nodesFileName) throws Exception {
        nodes = new TreeMap<>();

        initNodes(nodesFileName);
    }

    public Node getNode(BigInteger key) {
        Node node = nodes.get(key);

        if (Logger.VERBOSE_CIRCLE)
            Logger.log("retrieved node [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

        return node;
    }

    public BigInteger getNodeKey(byte[] key) {
        BigInteger nodeKey = hash(key);
        if (!nodes.containsKey(nodeKey)) {
            SortedMap<BigInteger, Node> tailMap = nodes.tailMap(nodeKey);
            nodeKey = tailMap.isEmpty() ? nodes.firstKey() : tailMap.firstKey();
        }

        return nodeKey;
    }

    public BigInteger getKey(String key) {
        return hash(key.getBytes());
    }

    public void remove(BigInteger key) {
        nodes.remove(key);
    }

    private BigInteger hash(byte[] key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ALGORITHM_HASH);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        md.update(key);
        BigInteger hash = new BigInteger(md.digest());
        md.reset();
        return hash;
    }

    private void initNodes(String nodesFileName) throws IOException {
        FileReader fileReader = new FileReader(nodesFileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        Node node;
        String line;
        String[] parts;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains(IGNORED_NODE))
                continue;

            parts = line.split(DELIMITER_NODE);
            node = new Node(InetAddress.getByName(parts[0]), Integer.parseInt(parts[1]));

            nodes.put(hash(line.getBytes()), node);

            if (Logger.VERBOSE_CIRCLE)
                Logger.log("added node [" + line + "]");
        }

        bufferedReader.close();
    }
}
