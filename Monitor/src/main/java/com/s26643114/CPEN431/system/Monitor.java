package com.s26643114.CPEN431.system;

import com.jcraft.jsch.JSchException;
import com.s26643114.CPEN431.model.Node;
import com.s26643114.CPEN431.protocol.Report;
import com.s26643114.CPEN431.util.SSHManager;

import java.io.IOException;

public class Monitor {
    private static final String COMMAND_DISK = "du -s";
    private static final String COMMAND_PING = "ping";
    private static final String COMMAND_UPTIME = "uptime";

    private static final String ERROR_EXEC = "Cannot execute command";
    private static final String ERROR_LOGIN = "Cannot log in";
    private static final String ERROR_ONLINE = "Not online";
    private static final String FALSE = "No";
    private static final String TRUE = "Yes";

    private static Node node;
    private static SSHManager ssh;
    private static String ip;
    private static String keyName;
    private static String keyPass;
    private static String port;
    private static String username;

    public static void main(String[] args) {
        ip = args[0];
        port = args[1];
        String hostName = args[2];
        String nodeId = args[3];
        username = args[4];
        keyName= args[5];
        keyPass = args[6];

        try {
            Process p = Runtime.getRuntime().exec(new String[] {COMMAND_PING, "-c", "3", hostName});
            if (p.waitFor() != 0) {
                node = new Node(hostName, nodeId, FALSE, ERROR_ONLINE, ERROR_ONLINE, ERROR_ONLINE);
                System.out.println("Failed ping");
                shutDown();
            }
        } catch (Exception e) {
            node = new Node(hostName, nodeId, ERROR_EXEC, ERROR_ONLINE, ERROR_ONLINE, ERROR_ONLINE);
            System.out.println("Ping exception");
            shutDown();
        }


        ssh = new SSHManager(hostName, username, keyName, keyPass);
        try {
            System.out.println("Connecting:" + hostName + ":" + username + ":" + keyName + ":" + keyPass);
            ssh.connect();
        } catch (JSchException e) {
            node = new Node(hostName, nodeId, TRUE, FALSE, ERROR_LOGIN, ERROR_LOGIN);
            System.out.println("Failed login");
            shutDown();
        }

        String disk;
        try {
            disk = ssh.exec(COMMAND_DISK);
            disk = disk.replace("\t", "").replace("\n", "").replace(".", "");
        } catch (Exception e) {
            System.out.println("Failed disk");
            disk = ERROR_EXEC;
        }

        String uptime;
        try {
            uptime = ssh.exec(COMMAND_UPTIME);
        } catch (Exception e) {
            System.out.println("Failed uptime");
            uptime = ERROR_EXEC;
        }

        node = new Node(hostName, nodeId, TRUE, TRUE, disk, uptime);

        sendNode();
    }

    private static void initNewNode(String newNode) throws IOException, JSchException {
        String[] nodeInfo = newNode.split("-");
        ssh.scpTo("id_rsa");
        ssh.scpTo("monitor.jar");
        ssh.exec("chmod 600 id_rsa");
        ssh.exec("nohup java -jar monitor.jar " + ip + " " + port + " " + nodeInfo[0] + " " + nodeInfo[1] + " "
                +  username + " " + keyName + " " + keyPass + " &");
    }

    private static void selfDestruct() {
        try {
            Runtime.getRuntime().exec(new String[] {"rm", "id_rsa"});
            Runtime.getRuntime().exec(new String[] {"rm", "monitor.jar"});
        } catch (IOException ignored) {}
    }

    private static void sendNode() {
        try {
            Report report = new Report(ip, port);
            String newNode = report.reportNode(node.toJSON());
            //if (newNode != null)
                //initNewNode(newNode);
        } catch (Exception ignored) {
        }
    }

    private static void shutDown() {
        sendNode();
        ssh.disconnect();
        //selfDestruct();
        System.exit(-1);
    }
}
