package com.daken.raft.core.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * TaskExecutor
 */
public interface TaskExecutor {

    /**
     * Submit task.
     *
     * @param task task
     * @return future
     */
    Future<?> submit(Runnable task);

    /**
     * Submit callable task.
     *
     * @param task task
     * @param <V>  result type
     * @return future
     */
    <V> Future<V> submit(Callable<V> task);

    /**
     * Shutdown.
     *
     * @throws InterruptedException if interrupted
     */
    void shutdown() throws InterruptedException;


}
