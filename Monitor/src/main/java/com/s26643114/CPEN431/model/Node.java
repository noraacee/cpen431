package com.s26643114.CPEN431.model;

import org.json.simple.JSONObject;

public class Node {
    private static final String DISK = "disk";
    private static final String HOST_NAME = "hostname";
    private static final String LOGIN = "login";
    private static final String NODE_ID = "nodeid";
    private static final String ONLINE = "online";
    private static final String UPTIME = "uptime";

    private JSONObject obj;

    private String disk;
    private String hostName;
    private String login;
    private String nodeId;
    private String online;
    private String uptime;

    public Node(String hostName, String nodeId,  String online, String login, String disk, String uptime) {
        this.hostName = hostName;
        this.nodeId = nodeId;
        this.online = online;
        this.login = login;
        this.disk = disk;
        this.uptime = uptime;
        createJSON();
    }

    public String getDisk() {
        return disk;
    }

    public String getHostName() { return hostName; }

    public String getLogin() {
        return login;
    }

    public String getNodeId() { return nodeId; }

    public String getOnline() {
        return online;
    }

    public String getUptime() {
        return uptime;
    }

    public void createJSON() {
        obj = new JSONObject();
        obj.put(DISK, disk);
        obj.put(HOST_NAME, hostName);
        obj.put(LOGIN, login);
        obj.put(NODE_ID, nodeId);
        obj.put(ONLINE, online);
        obj.put(UPTIME, uptime);
    }

    public String toJSON() {
        return obj.toJSONString();
    }
}
