package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteUtil;

import java.net.DatagramPacket;

public class Reply extends Protocol {
    public static byte[] createReply(DatagramPacket packet, byte[] uniqueId, byte errorCode) {
        byte[] reply = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + Long.BYTES];
        System.arraycopy(uniqueId, 0, reply, 0, LENGTH_UNIQUE_ID);

        reply[LENGTH_UNIQUE_ID] = errorCode;

        packet.setData(reply);
        packet.setLength(reply.length - Long.BYTES);

        return reply;
    }

    public static byte[] createReply(DatagramPacket packet, byte[] uniqueId, byte[] value) {
        int length = LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH + value.length;
        byte[] reply = new byte[length + Long.BYTES];

        int index = 0;

        System.arraycopy(uniqueId, 0, reply, index, LENGTH_UNIQUE_ID);
        index += LENGTH_UNIQUE_ID;

        reply[index] = ERROR_NONE;
        index += LENGTH_CODE;

        ByteUtil.int2leb(value.length, reply, index, LENGTH_VALUE_LENGTH);
        index += LENGTH_VALUE_LENGTH;

        System.arraycopy(value, 0, reply, index, value.length);

        packet.setData(reply);
        packet.setLength(length);

        return reply;
    }

    public static void setReply(DatagramPacket packet, byte errorCode) {
        packet.getData()[LENGTH_UNIQUE_ID] = errorCode;
        packet.setLength(LENGTH_UNIQUE_ID + LENGTH_CODE);
    }

    public static void setReply(DatagramPacket packet, byte[] reply) {
        packet.setData(reply);
        packet.setLength(reply.length - Long.BYTES);
    }
}
