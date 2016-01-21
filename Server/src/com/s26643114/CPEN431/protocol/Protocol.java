package com.s26643114.CPEN431.protocol;

public class Protocol {
    public static final int LENGTH_CODE = 1;
    public static final int LENGTH_KEY = 32;
    public static final int LENGTH_UNIQUE_ID = 16;
    public static final int LENGTH_VALUE = 10000;
    public static final int LENGTH_VALUE_LENGTH = 2;

    public static byte[] extractUniqueId(byte[] request) {
        byte[] uniqueId = new byte[LENGTH_UNIQUE_ID];
        System.arraycopy(request, 0, uniqueId, 0, LENGTH_UNIQUE_ID);

        return uniqueId;
    }
}
