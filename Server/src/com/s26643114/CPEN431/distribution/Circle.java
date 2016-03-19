package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.util.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Circle {
    private static final String ALGORITHM_HASH = "MD5";
    private static final String DELIMITER_NODE = ":";
    private static final String IGNORED_NODE = "#";

    private int index;
    private int indexSelf;

    private BigInteger self;
    private ConcurrentSkipListMap<BigInteger, Node> nodes;
    private List<BigInteger> keySet;
    private Map<BigInteger, Node> heartbeats;

    public Circle(String ip, String nodesFileName) throws Exception {
        nodes = new ConcurrentSkipListMap<>();
        keySet = new LinkedList<>();
        heartbeats = new HashMap<>();

        initNodes(ip, nodesFileName);
    }

    public void checkNodes() {
        for (Iterator<Map.Entry<BigInteger, Node>> it = heartbeats.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BigInteger, Node> entry = it.next();
            Node node = entry.getValue();
            if (node != null && !node.isAlive())
                remove(it, node.getKey());
        }
    }


    public boolean checkSelf(byte[] key) {
        return key == null || self.equals(getNodeKey(key));
    }

    public Map<BigInteger, Node> getHeartbeats() {
        return heartbeats;
    }

    public Node getNextNode() {
        index++;

        if (index == indexSelf) {
            index++;
            if (index >= keySet.size())
                index = 0;
        } else if (index >= keySet.size()) {
            index = 0;
            if (index == indexSelf)
                index++;
        }

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "retrieving node of index " + index);

        return getNode(keySet.get(index));
    }

    public Node getNode(BigInteger key) {
        Node node = nodes.get(key);

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "retrieved node [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

        return node;
    }

    public BigInteger getNodeKey(byte[] key) {
        BigInteger nodeKey = hash(key);

        if (nodeKey == null)
            return null;

        return getNodeKey(nodeKey);
    }

    public Node getSelf() {
        return nodes.get(self);
    }

    public void initNodes() {
        for (BigInteger key : keySet)
            nodes.get(key).init();

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "node heartbeats initialized");
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

    private void initNodes(String ipString, String nodesFileName) throws IOException {
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

            parts = line.trim().split(DELIMITER_NODE);
            node = new Node(InetAddress.getByName(parts[0]), Integer.parseInt(parts[1]));

            BigInteger nodeKey = hash(node.getIp().getAddress());
            if (nodeKey == null)
                continue;

            if (parts.length == 3 && ipString.equals(parts[2]))
                self = nodeKey;
            else if (ipString.equals(parts[0]))
                self = nodeKey;

            node.setKey(nodeKey);
            BigInteger ip = new BigInteger(node.getIp().getAddress());

            heartbeats.put(ip, node);
            keySet.add(nodeKey);
            nodes.put(nodeKey, node);

            if (Logger.VERBOSE_CIRCLE)
                Logger.log(Logger.TAG_CIRCLE, "added node [" + line + "]");
        }

        if (self == null)
            self = hash(InetAddress.getByName(ipString).getAddress());

        indexSelf = keySet.indexOf(self);
        index = indexSelf;

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "self initialized with index of " + indexSelf);

        bufferedReader.close();
    }

    private void remove(Iterator<Map.Entry<BigInteger, Node>> it, BigInteger nodeKey) {
        it.remove();
        keySet.remove(nodeKey);

        Node node = nodes.remove(nodeKey);

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "removed node [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

        indexSelf = keySet.indexOf(self);
        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "new index of self is " + indexSelf);
    }
}
