package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteOrder;
import com.s26643114.CPEN431.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles reply side of the protocol
 */
public class Reply extends Protocol implements Runnable {
    private ConcurrentHashMap<byte[], Integer> queue;

    public Reply(ConcurrentHashMap<byte[], Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while(true) {

        }
    }

    /**
     * Parses the reply from the server to get the secret code
     *
     * @param reply - reply from server
     * @return secret code
     */
    public static String parseReply(byte[] reply) {
        int length = getLength(reply);

        byte[] secretCode = new byte[length];

        System.arraycopy(reply, Protocol.LENGTH_UNIQUE_ID + Integer.BYTES, secretCode, 0, length);

        return StringUtils.byteArrayToHexString(secretCode);
    }

    /**
     * Retrieves the length of the byte array of the secret code
     *
     * @param reply - reply from server
     * @return length of the byte array of the secret code
     */
    private static int getLength(byte[] reply) {
        int length = ByteOrder.leb2int(reply, Protocol.LENGTH_UNIQUE_ID);

        System.out.println("Secret Code Length: " + length);

        return length;
    }
}
