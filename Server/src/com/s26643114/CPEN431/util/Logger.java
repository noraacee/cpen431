package com.s26643114.CPEN431.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Logger {
    public static final boolean VERBOSE = false;
    public static final boolean VERBOSE_BASE = VERBOSE && true;
    public static final boolean VERBOSE_CIRCLE = VERBOSE && true;
    public static final boolean VERBOSE_CLIENT = VERBOSE && true;
    public static final boolean VERBOSE_DATABASE = VERBOSE && true;
    public static final boolean VERBOSE_INTERNAL_SERVER = VERBOSE && true;
    public static final boolean VERBOSE_SERVER = VERBOSE && true;
    public static final boolean VERBOSE_REQUEST = VERBOSE && true;
    public static final boolean VERBOSE_RETRY = VERBOSE && true;
    public static final boolean VERBOSE_ROUTE = VERBOSE && true;

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

    public static void log(String tag, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        log(tag, sw.getBuffer().toString());
    }

    public static void log(String tag, String log) {
        try {
            if (tag != null)
                logger.write(tag + ":");
            logger.write(log + "\n");
            logger.flush();
        } catch (IOException ignored) {}
    }
}
