package com.s26643114.CPEN431.model;

public class Node extends NodeBase {
    private String disk;
    private String login;
    private String online;
    private String uptime;

    public Node(String hostName, String nodeId,  String online, String login, String disk, String uptime) {
        super(hostName, nodeId);
        this.online = online;
        this.login = login;
        this.disk = disk;
        this.uptime = uptime;
    }

    public String getDisk() {
        return disk;
    }

    public String getLogin() {
        return login;
    }

    public String getOnline() {
        return online;
    }

    public String getUptime() {
        return uptime;
    }
}
