package com.daken.raft.core.schedule;

import javax.annotation.Nonnull;

/**
 * Scheduler
 */
public interface Scheduler {

    /**
     * 创建日志复制定时任务
     *
     * @param task task
     * @return LogReplicationTask
     */
    @Nonnull
    LogReplicationTask scheduleLogReplicationTask(@Nonnull Runnable task);


    /**
     * 创建选举超时定时任务
     *
     * @param task task
     * @return ElectionTimeout
     */
    @Nonnull
    ElectionTimeout scheduleElectionTimeout(@Nonnull Runnable task);


    /**
     * 取消定时任务
     */
    void stop() throws InterruptedException;

}
