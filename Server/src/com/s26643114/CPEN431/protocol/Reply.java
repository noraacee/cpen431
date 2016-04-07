package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.util.ByteUtil;
import com.s26643114.CPEN431.util.Logger;

import java.net.DatagramPacket;

public class Reply extends Protocol {

    /**
     * Creates a new reply to be routed internally, where destination ip and port are appended at the end of the request
     */
    public static byte[] createInternalReply(DatagramPacket packet) {
        long start;
        if (Logger.BENCHMARK_REPLY)
            start = System.nanoTime();

        byte[] reply = packet.getData();
        int length = packet.getLength() + LENGTH_IP + LENGTH_PORT;

        byte[] ip = packet.getAddress().getAddress();
        System.arraycopy(ip, 0, reply, packet.getLength(), LENGTH_IP);
        ByteUtil.int2leb(packet.getPort(), reply, packet.getLength() + LENGTH_IP, LENGTH_PORT);

        packet.setLength(length);

        if (Logger.VERBOSE_REPLY)
            Logger.log(Logger.TAG_REPLY, "internal reply to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "]: " + packet.getLength());

        if (Logger.BENCHMARK_REPLY) {
            long end = System.nanoTime();
            Logger.benchmark(Logger.TAG_REPLY, start, end, "internal");
        }

        return reply;
    }

    /**
     * Creates a new reply with given unique id and error code
     */
    public static byte[] createReply(DatagramPacket packet, byte[] uniqueId, byte errorCode) {
        long start;
        if (Logger.BENCHMARK_REPLY)
            start = System.nanoTime();

        byte[] reply = new byte[LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_INSTANT];
        System.arraycopy(uniqueId, 0, reply, 0, LENGTH_UNIQUE_ID);

        reply[LENGTH_UNIQUE_ID] = errorCode;

        packet.setData(reply);
        packet.setLength(reply.length - LENGTH_INSTANT);

        if (Logger.VERBOSE_REPLY)
            Logger.log(Logger.TAG_REPLY, "reply to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "]: " + packet.getLength());

        if (Logger.BENCHMARK_REPLY) {
            long end = System.nanoTime();
            Logger.benchmark(Logger.TAG_REPLY, start, end, "error code reply");
        }

        return reply;
    }

    /**
     * Creates a new reply with given unique id and return value
     */
    public static byte[] createReply(DatagramPacket packet, byte[] uniqueId, byte[] value) {
        long start;
        if (Logger.BENCHMARK_REPLY)
            start = System.nanoTime();

        int length = LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH + value.length;
        byte[] reply = new byte[length + LENGTH_INSTANT];

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

        if (Logger.VERBOSE_REPLY)
            Logger.log(Logger.TAG_REPLY, "reply to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "]: " + packet.getLength());

        if (Logger.BENCHMARK_REPLY) {
            long end = System.nanoTime();
            Logger.benchmark(Logger.TAG_REPLY, start, end, "value reply");
        }

        return reply;
    }

    /**
     * Sets the reply in the packet with given error code
     */
    public static void setReply(DatagramPacket packet, byte errorCode) {
        packet.getData()[LENGTH_UNIQUE_ID] = errorCode;
        packet.setLength(LENGTH_UNIQUE_ID + LENGTH_CODE);

        if (Logger.VERBOSE_REPLY)
            Logger.log(Logger.TAG_REPLY, "reply to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "]: " + packet.getLength());
    }

    /**
     * Sets the reply in the packet with cached reply
     */
    public static void setReply(DatagramPacket packet, byte[] reply) {
        packet.setData(reply);
        packet.setLength(reply.length - LENGTH_INSTANT);

        if (Logger.VERBOSE_REPLY)
            Logger.log(Logger.TAG_REPLY, "reply to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "]: " + packet.getLength());
    }


    /**
     * Sets the reply in the packet with pid
     */
    public static void setReply(DatagramPacket packet, int pid) {
        packet.getData()[LENGTH_UNIQUE_ID] = ERROR_NONE;
        ByteUtil.int2leb(LENGTH_PID, packet.getData(), LENGTH_UNIQUE_ID + LENGTH_CODE, LENGTH_VALUE_LENGTH);
        ByteUtil.int2leb(pid, packet.getData(), LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH, LENGTH_PID);
        packet.setLength(LENGTH_UNIQUE_ID + LENGTH_CODE + LENGTH_VALUE_LENGTH + LENGTH_PID);

        if (Logger.VERBOSE_REPLY)
            Logger.log(Logger.TAG_REPLY, "reply to [" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                    + "]: (pid)" + pid);
    }
}
