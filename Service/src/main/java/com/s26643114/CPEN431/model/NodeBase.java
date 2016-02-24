package com.s26643114.CPEN431.model;

public class NodeBase {
    private String hostName;
    private String nodeId;

    public NodeBase(NodeBase nodeBase) {
        this.hostName = nodeBase.getHostName();
        this.nodeId = nodeBase.getNodeId();
    }

    public NodeBase(String hostName, String nodeId) {
        this.hostName = hostName;
        this.nodeId = nodeId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getNodeId() {
        return nodeId;
    }

}
