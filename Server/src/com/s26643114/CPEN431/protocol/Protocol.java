package com.s26643114.CPEN431.protocol;

/**
 * Helper class with constants
 */
public class Protocol {
    /**
     * Command to execute
     */
    public static final byte MASK_COMMAND = 0x10;
    public static final byte COMMAND_PUT = 0x01;
    public static final byte COMMAND_GET = 0x02;
    public static final byte COMMAND_REMOVE = 0x03;
    public static final byte COMMAND_SHUTDOWN = 0x04;
    public static final byte COMMAND_REMOVE_ALL = 0x05;

    /**
     * Error code for reply
     */
    public static final byte ERROR_NONE = 0x00;     // operation is successful
    public static final byte ERROR_KEY = 0x01;      // non-existent key requested in a get or delete operation
    public static final byte ERROR_MEMORY = 0x02;   // out of memory for put operation
    //public static final byte ERROR_OVERLOAD = 0x03; // system overload
    public static final byte ERROR_FAILURE = 0x04;  // internal failure
    public static final byte ERROR_COMMAND = 0x05;  // unrecognized command
    public static final byte ERROR_LENGTH = 0x06;   // value length is invalid (cannot be less than 1 or more than 10000
    public static final byte ERROR_VALUE = 0x07;    // invalid value field
    public static final byte ERROR_PACKET = 0x08;   // packet is not structured as expected
    //public static final byte ERROR_ROUTING = 0x09;  // routing request to another node failed

    /**
     * Lengths of components of reply
     */
    public static final int LENGTH_CODE = 1;
    public static final int LENGTH_INSTANT = Long.BYTES;
    public static final int LENGTH_IP = 4;
    public static final int LENGTH_KEY = 32;
    public static final int LENGTH_PORT = 2;
    public static final int LENGTH_UNIQUE_ID = 16;
    public static final int LENGTH_VALUE = 10000;
    public static final int LENGTH_VALUE_LENGTH = 2;
    public static final int LENGTH_TOTAL = LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH
            + LENGTH_VALUE + LENGTH_IP + LENGTH_PORT + LENGTH_INSTANT;

    public static final int MAX_STORE = 100000;      // max number of key-value pairs in store
    public static final int RETRIES = 3;             // number of retries for crashed node
    public static final int SIZE_POOL_CLIENT = 72;   // size of pool of clients to reuse
    public static final int SIZE_POOL_THREAD = 36;   // size of pool of threads to reuse
    public static final int TIMEOUT = 5000;          // time for reply cache and internal request
}
