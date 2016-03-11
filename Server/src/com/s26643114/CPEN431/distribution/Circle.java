package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.util.Logging;

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

    private SortedMap<BigInteger, Node> nodes;

    public Circle(String nodesFileName) throws Exception {
        nodes = new TreeMap<>();

        initNodes(nodesFileName);
    }

    public Node getNode(BigInteger key) {
        Node node = nodes.get(key);

        if (Logging.VERBOSE_CIRCLE)
            Logging.log("retrieved node [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

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
            parts = line.split(":");
            node = new Node(InetAddress.getByName(parts[0]), Integer.parseInt(parts[1]));

            nodes.put(hash(line.getBytes()), node);

            if (Logging.VERBOSE_CIRCLE)
                Logging.log("added node [" + line + "]");
        }

        bufferedReader.close();
    }
}
