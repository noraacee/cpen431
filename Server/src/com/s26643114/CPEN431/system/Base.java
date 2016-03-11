package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Distribution;
import com.s26643114.CPEN431.util.Logging;

import java.net.InetAddress;

/**
 * Base with key-value store service
 */
public class Base {
    private static final int PORT = 12664;

    //private static final String ERROR_INIT = "An error has occured while initializing the distribution server. System returned with error: ";
    //private static final String ERROR_NUMBER_FORMAT = "Port must be an integer";
    //private static final String ERROR_SOCKET = "An error has occurred while creating the socket. System returned with error: ";
    //private static final String ERROR_UNKNOWN_HOST = "Cannot determine ip host";

    private static final String FILE_NAME_NODES = "servers.txt";

    public static void main(String[] args) {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            int port;
            String nodesFileName;

            switch (args.length) {
                case 1:
                    nodesFileName = args[0];
                    port = PORT;
                    break;
                case 2:
                    nodesFileName = args[0];
                    port = Integer.parseInt(args[1]);
                    break;
                default:
                    nodesFileName = FILE_NAME_NODES;
                    port = PORT;
                    break;
            }

            if (Logging.VERBOSE_BASE) {
                Logging.log("server started [" + ip.getHostAddress() + ":" + port + "]");
                Logging.log("servers list file name: " + nodesFileName);
            }

            Server server = new Server(ip, port);
            Distribution.init(server, nodesFileName);
            server.accept();
        } catch (Exception e) {
            if (Logging.VERBOSE_BASE)
                Logging.log(e);
        }
    }
}
