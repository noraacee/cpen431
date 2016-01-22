package com.s26643114.CPEN431.protocol;

/**
 * Helper class with constants
 */
public class Protocol {
    protected static final int LENGTH_CODE = 1;
    protected static final int LENGTH_KEY = 32;
    protected static final int LENGTH_UNIQUE_ID = 16;
    protected static final int LENGTH_VALUE = 10000;
    protected static final int LENGTH_VALUE_LENGTH = 2;

    protected static final int MAX_STORE = 100000; //max number of key-value pairs in store

    protected static final int RETRIES = 3;

    protected static final int TIMEOUT = 100; //Can be changed to longer according to client
}
