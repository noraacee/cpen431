package com.s26643114.CPEN431.system;

import com.s26643114.CPEN431.distribution.Route;
import com.s26643114.CPEN431.protocol.Protocol;
//import com.s26643114.CPEN431.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternalServer extends Thread {
    //private static final String TAG = "internal_server";

    private AtomicBoolean shutdown;
    private Database database;
    private DatagramSocket internalServer;

    public InternalServer(AtomicBoolean shutdown, Database database) throws SocketException {
        this.shutdown = shutdown;
        this.database = database;
        internalServer = new DatagramSocket();
    }

    @Override
    public void interrupt() {
        super.interrupt();

        //if (Logger.VERBOSE_INTERNAL_SERVER)
            //Logger.log(TAG, "shutting down internal server");
        internalServer.close();
    }

    @Override
    public void run() {
        DatagramPacket packet = new DatagramPacket(new byte[Protocol.LENGTH_INSTANT], Protocol.LENGTH_INSTANT);
        AtomicBoolean lock;
        while(!shutdown.get()) {
            try {
                internalServer.receive(packet);

                long lockKey = Route.convertKey(packet);

                //if (Logger.VERBOSE_INTERNAL_SERVER)
                    //Logger.log(TAG, "acknowledgement received from [" + packet.getAddress().getHostAddress() + ":"
                            //+ packet.getPort() + "] with key: " + lockKey);

                synchronized (lock = database.remove(lockKey)) {
                    lock.notify();
                }
            } catch (IOException e) {
                //if (Logger.VERBOSE_INTERNAL_SERVER)
                    //Logger.log(TAG, e);
                internalServer.close();
                shutdown.set(true);
                break;
            } catch (Exception e) {
                //if (Logger.VERBOSE_INTERNAL_SERVER)
                    //Logger.log(TAG, e);
            }
        }
    }

    public void send(DatagramPacket packet, long key, AtomicBoolean lock) throws IOException {
        database.put(key, lock);
        internalServer.send(packet);
    }
}
