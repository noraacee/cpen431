package com.s26643114.CPEN431.util;

import com.jcraft.jsch.*;

import java.io.*;

public class SSHManager {
    private static final int PORT_SSH = 22;
    private static final int SIZE_BUFFER = 1024;

    private static final String COMMAND_EXEC = "exec";
    private static final String COMMAND_FILE_SIZE = "C0644 %d %s \n";
    private static final String COMMAND_SCP = "scp -t %s";
    private static final String COMMAND_SHELL = "shell";

    private Session session;

    private String host;
    private String keyPass;
    private String keyName;
    private String username;

    public SSHManager(String host, String username, String keyName, String keyPass) {
        this.host = host;
        this.username = username;
        this.keyName = keyName;
        this.keyPass = keyPass;

        JSch.setConfig("StrictHostKeyChecking", "no");
    }

    public void connect() throws JSchException {
        JSch ssh = new JSch();
        ssh.addIdentity(keyName, keyPass);

        session = ssh.getSession(username, host, PORT_SSH);
        session.connect();
    }

    public void disconnect() {
        session.disconnect();
    }

    public String exec(String command) throws JSchException, IOException {
        Channel channel = session.openChannel(COMMAND_EXEC);

        BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        ((ChannelExec) channel).setCommand(command);
        channel.connect();

        String msg = in.readLine();

        channel.disconnect();

        return msg;
    }

    public boolean scpTo(String fileName) throws JSchException, IOException {
        FileInputStream fStream = null;

        try {
            String command = COMMAND_EXEC;
            Channel channel = session.openChannel(command);

            command = String.format(COMMAND_SCP, fileName);
            ((ChannelExec) channel).setCommand(command);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if (in.read() != 0)
                return false;

            File file = new File(fileName);
            long fileSize = file.length();
            command = String.format(COMMAND_FILE_SIZE, fileSize, fileName);

            out.write(command.getBytes());
            out.flush();

            if (in.read() != 0)
                return false;

            fStream = new FileInputStream(file);
            byte[] buf = new byte[SIZE_BUFFER];
            int len;
            while(true) {
                len = fStream.read(buf, 0, buf.length);
                if (len <= 0)
                    break;
                out.write(buf, 0, len);
            }

            fStream.close();
            fStream = null;

            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            if (in.read() != 0)
                return false;

            out.close();
            channel.disconnect();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (fStream != null) {
                try {
                    fStream.close();
                } catch (IOException ignored) {}
            }

            return false;
        }
    }
}
