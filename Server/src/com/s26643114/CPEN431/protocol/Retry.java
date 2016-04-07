package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.system.Database;
import com.s26643114.CPEN431.util.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles client's retries
 */
public class Retry extends Thread {
    private volatile boolean running;

    private Database database;

    public Retry(Database database) {
        this.database = database;
        running = false;

        if (Logger.VERBOSE_RETRY)
            Logger.log(Logger.TAG_RETRY, "started");
    }

    /**
     * Waits to see if client wants to retry and resends reply. Removes itself from timeCache when timeout or retries run out.
     */
    @Override
    public void run() {
        running = true;

        long timeout;
        while(running) {
            timeout = database.getTimeout();

            if (timeout > 0) {
                synchronized (this) {
                    try {
                        if (Logger.VERBOSE_RETRY)
                            Logger.log(Logger.TAG_RETRY, "wait: " + timeout + " ms");

                        wait(timeout);
                    } catch (InterruptedException e) {
                        if (Logger.VERBOSE_RETRY)
                            Logger.log(Logger.TAG_RETRY, "interrupted");
                        break;
                    }
                }
            }

            database.removeCache();
        }

        if (Logger.VERBOSE_RETRY)
            Logger.log(Logger.TAG_RETRY, "stopped");
    }

    public void exit() {
        running = false;
        interrupt();
    }
}
