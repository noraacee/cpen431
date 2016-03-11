package com.s26643114.CPEN431.protocol;

/**
 * Helper class with constants
 */
public class Protocol {
    /**
     * Command to execute
     */
    protected static final byte MASK_COMMAND = 0x10;
    protected static final byte COMMAND_PUT = 0x01;
    protected static final byte COMMAND_GET = 0x02;
    protected static final byte COMMAND_REMOVE = 0x03;
    protected static final byte COMMAND_SHUTDOWN = 0x04;
    protected static final byte COMMAND_REMOVE_ALL = 0x05;

    /**
     * Error code for reply
     */
    protected static final byte ERROR_NONE = 0x00;     // operation is successful
    protected static final byte ERROR_KEY = 0x01;      // non-existent key requested in a get or delete operation
    protected static final byte ERROR_MEMORY = 0x02;   // out of memory for put operation
    //protected static final byte ERROR_OVERLOAD = 0x03; // system overload
    protected static final byte ERROR_FAILURE = 0x04;  // internal failure
    protected static final byte ERROR_COMMAND = 0x05;  // unrecognized command
    protected static final byte ERROR_LENGTH = 0x06;   // value length is invalid (cannot be less than 1 or more than 10000
    protected static final byte ERROR_VALUE = 0x07;    // invalid value field
    protected static final byte ERROR_PACKET = 0x08;   // packet is not structured as expected
    //protected static final byte ERROR_ROUTING = 0x09;  // routing request to another node failed

    /**
     * Lengths of components of reply
     */
    protected static final int LENGTH_CODE = 1;
    protected static final int LENGTH_IP = 4;
    protected static final int LENGTH_KEY = 32;
    protected static final int LENGTH_PORT = 2;
    protected static final int LENGTH_UNIQUE_ID = 16;
    protected static final int LENGTH_VALUE = 10000;
    protected static final int LENGTH_VALUE_LENGTH = 2;
    protected static final int LENGTH_TOTAL = LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_KEY + LENGTH_VALUE_LENGTH + LENGTH_VALUE + LENGTH_IP + LENGTH_PORT;

    protected static final int MAX_STORE = 100000; // max number of key-value pairs in store
    protected static final int TIMEOUT = 5000;     // time to cache reply

    /**
     * Internal error messages
     */
    //protected static final String ERROR_MESSAGE_RECEIVE = "Unable to receive request. System returned with error: ";
    //protected static final String ERROR_MESSAGE_SEND = "Unable to send reply. System returned with error: ";
}
