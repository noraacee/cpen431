package com.s26643114.CPEN431.distribution;

import com.s26643114.CPEN431.protocol.Protocol;
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

    private int indexSelf;

    private BigInteger self;
    private ConcurrentSkipListMap<BigInteger, Node> nodes;
    private List<Node> keySet;
    private Map<BigInteger, Node> heartbeats;
    private Random random;

    public Circle(String ip, String nodesFileName) throws Exception {
        indexSelf = 0;

        nodes = new ConcurrentSkipListMap<>();
        keySet = new ArrayList<>();
        heartbeats = new HashMap<>();
        random = new Random();

        initNodes(ip, nodesFileName);
    }

    /**
     * Iterate through the nodes list to check if any nodes are dead, and if so, remove it
     */
    public void checkNodes() {
        for (int i = 0; i < heartbeats.size(); i++) {
            Node node = keySet.get(i);
            if (node != null && !node.isAlive())
                remove(i, node.getNodeKey(), node.getHeartbeatKey());
        }
    }

    /**
     * Get the heartbeats of all nodes to send
     */
    public Map<BigInteger, Node> getHeartbeats() {
        return heartbeats;
    }

    /**
     * Get the next node to send heartbeats to
     */
    public Node getNextNode() {
        int index = random.nextInt(keySet.size());

        if (index == indexSelf) {
            index++;
            if (index >= keySet.size())
                index = 0;
        }

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "retrieving node of index " + index);

        return keySet.get(index);
    }

    /**
     * Gets the node corresponding to the key
     */
    public Node getNode(byte[] key, Node[] replicas) {
        BigInteger nodeKey = hash(key);
        if (nodeKey == null)
            return null;

        try {
            ConcurrentNavigableMap<BigInteger, Node> tailMap = nodes.tailMap(nodeKey);
            if (tailMap.isEmpty()) {
                for (int i = 0; i < Protocol.REPLICATION; i++) {
                    Node n = keySet.get(i);
                    if (Logger.VERBOSE_CIRCLE)
                        Logger.log(Logger.TAG_CIRCLE, "replicate node: [" + n.getIp().getHostAddress() + ":" + n.getPort() + "]");

                    replicas[i] = n;
                }
            } else {
                int i = 0;
                for (Node n : tailMap.values()) {
                    if (i == Protocol.REPLICATION)
                        break;

                    replicas[i] = n;
                    if (Logger.VERBOSE_CIRCLE)
                        Logger.log(Logger.TAG_CIRCLE, "replicate node: [" + n.getIp().getHostAddress() + ":" + n.getPort() + "]");

                    i++;
                }

                for (int j = 0; i + j < Protocol.REPLICATION; j++) {
                    Node n = keySet.get(j);
                    if (Logger.VERBOSE_CIRCLE)
                        Logger.log(Logger.TAG_CIRCLE, "replicate node: [" + n.getIp().getHostAddress() + ":" + n.getPort() + "]");

                    replicas[i + j] = n;
                }
            }
        } catch (NoSuchElementException e) {
            return getNode(key, replicas);
        }

        return replicas[0];
    }

    /**
     * Gets the self key
     */
    public BigInteger getSelfKey() {
        return self;
    }

    /**
     * Gets the self node
     */
    public Node getSelfNode() {
        return nodes.get(self);
    }

    /**
     * Initialize timer for heartbeats on all nodes
     */
    public void initHeartbeats() {
        keySet.forEach(Node::init);

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "node heartbeats initialized");
    }

    /**
     * Hash a request key
     */
    private BigInteger hash(byte[] key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ALGORITHM_HASH);
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }

        md.update(key);
        return new BigInteger(md.digest());
    }

    /**
     * Parses and initializes the nodes list
     */
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

            InetAddress ip = InetAddress.getByName(parts[0]);
            BigInteger nodeKey = hash(ip.getAddress());
            if (nodeKey == null)
                continue;

            BigInteger heartbeatKey = new BigInteger(ip.getAddress());

            node = new Node(ip, Integer.parseInt(parts[1]), nodeKey, heartbeatKey);

            if (ipString.equals(ip.getHostAddress()))
                self = nodeKey;

            heartbeats.put(heartbeatKey, node);
            nodes.put(nodeKey, node);

            if (Logger.VERBOSE_CIRCLE)
                Logger.log(Logger.TAG_CIRCLE, "added node [" + line + "]");
        }

        if (self == null)
            self = hash(InetAddress.getByName(ipString).getAddress());

        keySet.addAll(nodes.values());

        if (Logger.VERBOSE) {
            for (int i = 0; i < keySet.size(); i++)
                Logger.log(Logger.TAG_CIRCLE, "node " + i + ": " + keySet.get(i).getIp().getHostAddress());
        }

        for (int i = 0; i < keySet.size(); i++) {
            if (self.equals(keySet.get(i).getNodeKey())) {
                indexSelf = i;
                break;
            }
        }

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "self initialized with index of " + indexSelf);

        bufferedReader.close();
    }

    /**
     * Removes a node from the nodes list
     */
    private void remove(int index, BigInteger nodeKey, BigInteger heartbeatKey) {
        keySet.remove(index);
        heartbeats.remove(heartbeatKey);
        Node node = nodes.remove(nodeKey);

        if (Logger.VERBOSE)
            Logger.log(Logger.TAG_CIRCLE, "removed node [" + node.getIp().getHostAddress() + ":" + node.getPort() + "]");

        if (index < indexSelf)
            indexSelf--;

        if (Logger.VERBOSE_CIRCLE)
            Logger.log(Logger.TAG_CIRCLE, "new index of self is " + indexSelf);
    }
}
