package com.s26643114.CPEN431.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Logger {
    public static final boolean VERBOSE_BASE = true;
    public static final boolean VERBOSE_CIRCLE = true;
    public static final boolean VERBOSE_CLIENT = true;
    public static final boolean VERBOSE_DISTRIBUTION = true;
    public static final boolean VERBOSE_SERVER = true;
    public static final boolean VERBOSE_REQUEST = true;

    public static final String FILENAME_LOG = "server.log";

    private static FileWriter logger;

    public static void close() {
        try {
            logger.close();
        } catch (IOException ignored) {}
    }

    public static void init() throws IOException {
        PrintWriter writer = new PrintWriter(FILENAME_LOG, StandardCharsets.UTF_8.toString());
        writer.close();
        logger = new FileWriter(FILENAME_LOG, true);
    }

    public static void log(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        log(sw.getBuffer().toString());
    }

    public static void log(String log) {
        try {
            logger.write(log + "\n");
            logger.flush();
        } catch (IOException ignored) {}
    }
}
