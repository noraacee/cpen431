package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Distribution;
import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base with key-value store service
 */
public class Base {
    private static final int PORT = 12664;

    //private static final String ERROR_INIT = "An error has occured while initializing the distribution server. System returned with error: ";
    //private static final String ERROR_NUMBER_FORMAT = "Port must be an integer";
    //private static final String ERROR_SOCKET = "An error has occurred while creating the socket. System returned with error: ";
    //private static final String ERROR_UNKNOWN_HOST = "Cannot determine ip host";

    private static final String FILENAME_NODES = "nodes.list";
    private static final String PATTERN_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    private static final String TIME_ZONE = "UTC-08:00";

    public static void main(String[] args) {
        try {
            Logger.init();
        } catch (IOException e) {
            return;
        }

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
                    nodesFileName = FILENAME_NODES;
                    port = PORT;
                    break;
            }

            if (Logger.VERBOSE_BASE) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(PATTERN_DATE_TIME);
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TIME_ZONE));

                Logger.log("[" + ip.getHostAddress() + ":" + port + "] at [" + now.format(dtf) + "]");
            }

            Server server = new Server(ip, port);
            Distribution.init(server, nodesFileName);
            server.accept();
        } catch (Exception e) {
            if (Logger.VERBOSE_BASE)
                Logger.log(e);
        }

        Logger.close();
    }
}
