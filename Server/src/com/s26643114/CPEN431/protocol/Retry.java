package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.system.Database;
import com.s26643114.CPEN431.util.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles client's retries
 */
public class Retry extends Thread {
    private static final String TAG = "retry";

    private AtomicBoolean shutdown;
    private Database database;

    public Retry(AtomicBoolean shutdown, Database database) {
        this.shutdown = shutdown;
        this.database = database;
    }

    /**
     * Waits to see if client wants to retry and resends reply. Removes itself from timeCache when timeout or retries run out.
     */
    @Override
    public void run() {
        long timeout;

        while(!shutdown.get()) {
            timeout = database.getTimeout();

            if (timeout > 0) {
                synchronized (this) {
                    try {
                        if (Logger.VERBOSE_RETRY)
                            Logger.log(TAG, "waiting for " + timeout + " ms");
                        wait(timeout);
                    } catch (InterruptedException e) {
                        if (Logger.VERBOSE_RETRY)
                            Logger.log(TAG, "retry thread interrupted");
                        break;
                    }
                }
            }

            if (Logger.VERBOSE_RETRY)
                Logger.log(TAG, "removing retry after waiting for " + timeout + " ms");
            database.removeCache();
        }

        if (Logger.VERBOSE_RETRY)
            Logger.log(TAG, "retry thread stopped");
    }
}
