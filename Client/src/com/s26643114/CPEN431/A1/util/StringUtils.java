package com.s26643114.CPEN431.A1.util;

/**
 * Various static routines to help with strings
 */
public class StringUtils {

    /**
     * Converts byte array to hex string
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder();
        String str;
        int val;

        for (byte aByte : bytes) {
            val = ByteOrder.ubyte2int(aByte);
            str = Integer.toHexString(val);
            while (str.length() < 2)
                str = "0" + str;
            buf.append(str);
        }
        return buf.toString().toUpperCase();
    }
}
