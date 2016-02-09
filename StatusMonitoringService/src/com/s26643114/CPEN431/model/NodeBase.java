package com.s26643114.CPEN431.model;

public class NodeBase {
    private String nodeId;
    private String hostName;

    public NodeBase(String hostName, String nodeId) {
        this.hostName = hostName;
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHostName() {
        return hostName;
    }
}
