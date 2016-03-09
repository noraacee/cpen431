package com.s26643114.CPEN431.protocol;

import com.s26643114.CPEN431.system.Database;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles client's retries
 */
public class Retry extends Protocol implements Runnable {
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
                        wait(timeout);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            database.removeFirst();
        }
    }
}
