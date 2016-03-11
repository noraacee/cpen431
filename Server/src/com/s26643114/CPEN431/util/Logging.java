package com.s26643114.CPEN431.util;

public class Logging {
    public static final boolean VERBOSE_BASE = true;
    public static final boolean VERBOSE_CIRCLE = true;
    public static final boolean VERBOSE_CLIENT = true;
    public static final boolean VERBOSE_DISTRIBUTION = true;
    public static final boolean VERBOSE_SERVER = true;
    public static final boolean VERBOSE_REQUEST = true;

    public static void log(Exception e) {
        e.printStackTrace();
    }

    public static void log(VirtualMachineError e) {
        e.printStackTrace();
    }

    public static void log(String log) {
        System.out.println(log);
    }
}
