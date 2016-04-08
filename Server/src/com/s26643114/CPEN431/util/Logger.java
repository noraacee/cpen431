package com.s26643114.CPEN431.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Logger {
    public static final boolean BENCHMARK             = false;
    public static final boolean BENCHMARK_CLIENT      = BENCHMARK && false;
    public static final boolean BENCHMARK_HEARTBEAT   = BENCHMARK && false;
    public static final boolean BENCHMARK_INTERNAL    = BENCHMARK && true;
    public static final boolean BENCHMARK_REPLICATION = BENCHMARK && false;
    public static final boolean BENCHMARK_REPLY       = BENCHMARK && false;
    public static final boolean BENCHMARK_REQUEST     = BENCHMARK && false;
    public static final boolean BENCHMARK_ROUTE       = BENCHMARK && false;
    public static final boolean BENCHMARK_SERVER      = BENCHMARK && true;

    public static final boolean VERBOSE             = false;
    public static final boolean VERBOSE_BASE        = VERBOSE && false;
    public static final boolean VERBOSE_CIRCLE      = VERBOSE && false;
    public static final boolean VERBOSE_CLIENT      = VERBOSE && false;
    public static final boolean VERBOSE_DATABASE    = VERBOSE && false;
    public static final boolean VERBOSE_HEARTBEAT   = VERBOSE && false;
    public static final boolean VERBOSE_INTERNAL    = VERBOSE && true;
    public static final boolean VERBOSE_REPLICATION = VERBOSE && true;
    public static final boolean VERBOSE_REPLY       = VERBOSE && false;
    public static final boolean VERBOSE_REQUEST     = VERBOSE && true;
    public static final boolean VERBOSE_RETRY       = VERBOSE && false;
    public static final boolean VERBOSE_ROUTE       = VERBOSE && true;
    public static final boolean VERBOSE_SERVER      = VERBOSE && true;

    public static final String FILENAME_LOG = "server.log";

    public static final String TAG_BASE        = "BASE";
    public static final String TAG_CIRCLE      = "CIRCLE";
    public static final String TAG_CLIENT      = "CLIENT";
    public static final String TAG_DATABASE    = "DATABASE";
    public static final String TAG_HEARTBEAT   = "HEARTBEAT";
    public static final String TAG_INTERNAL    = "INTERNAL";
    public static final String TAG_REPLICATION = "REPLICATION";
    public static final String TAG_REPLY       = "REPLY";
    public static final String TAG_REQUEST     = "REQUEST";
    public static final String TAG_RETRY       = "RETRY";
    public static final String TAG_ROUTE       = "ROUTE";
    public static final String TAG_SERVER      = "SERVER";

    private static final int LENGTH_LONGEST = 11;

    private static FileWriter logger;

    /**
     * Closes the logger
     */
    public static void close() {
        try {
            logger.close();
        } catch (IOException ignored) {}
    }

    /**
     * Initializes the logger
     */
    public static void init() throws IOException {
        PrintWriter writer = new PrintWriter(FILENAME_LOG, StandardCharsets.UTF_8.toString());
        writer.close();
        logger = new FileWriter(FILENAME_LOG, true);
    }

    /**
     * Logs a benchmark
     */
    public static void benchmark(String tag, long start, long end, String event) {
        log(tag, event + " : " + (end - start) / 1000 + " us");
    }

    /**
     * Logs an exception or error
     */
    public static void log(String tag, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        log(tag, sw.getBuffer().toString());
    }

    /**
     * Logs an event
     */
    public static void log(String tag, String log) {
        try {
            if (tag != null) {
                logger.write(System.currentTimeMillis() + "|");
                for (int i = tag.length(); i < LENGTH_LONGEST; i++)
                    logger.write(" ");
                logger.write("[" + tag + "] | ");
            }
            logger.write(log + "\n");
            logger.flush();
        } catch (IOException ignored) {}
    }
}
