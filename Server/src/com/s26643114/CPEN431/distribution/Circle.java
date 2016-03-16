package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.util.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Circle {
    private static final String TAG = "circle";

    private static final String ALGORITHM_HASH = "MD5";
    private static final String DELIMITER_NODE = ":";
    private static final String IGNORED_NODE = "#";

    private ConcurrentSkipListMap<BigInteger, Node> nodes;

    public Circle(String nodesFileName) throws Exception {
        nodes = new ConcurrentSkipListMap<>();

        initNodes(nodesFileName);
    }

    public Node getNode(BigInteger key) {
        Node node = nodes.get(key);

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(TAG, "retrieved node [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

        return node;
    }

    public BigInteger getNodeKey(byte[] key) {
        BigInteger nodeKey = hash(key);

        if (nodeKey == null)
            return null;

        return getNodeKey(nodeKey);
    }

    public BigInteger getKey(String key) {
        return hash(key.getBytes());
    }

    public void remove(BigInteger key) {
        nodes.remove(key);
    }

    private BigInteger getNodeKey(BigInteger key) {
        try {
            if (!nodes.containsKey(key)) {
                ConcurrentNavigableMap<BigInteger, Node> tailMap = nodes.tailMap(key);
                key = tailMap.isEmpty() ? nodes.firstKey() : tailMap.firstKey();
            }
        } catch (NoSuchElementException e) {
            return getNodeKey(key);
        }

        return key;
    }

    private BigInteger hash(byte[] key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ALGORITHM_HASH);
        } catch (NoSuchAlgorithmException ignored) {
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

            if (!line.contains(DELIMITER_NODE))
                continue;

            parts = line.split(DELIMITER_NODE);
            node = new Node(InetAddress.getByName(parts[0]), Integer.parseInt(parts[1]));

            BigInteger nodeKey = hash(line.getBytes());
            if (nodeKey == null)
                continue;

            nodes.put(nodeKey, node);

            if (Logger.VERBOSE_CIRCLE)
                Logger.log(TAG, "added node [" + line + "]");
        }

        bufferedReader.close();
    }
}
