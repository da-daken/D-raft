package com.daken.raft.core.schedule;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

/**
 * NullScheduler
 */
@Slf4j
public class NullScheduler implements Scheduler {

    @Nonnull
    @Override
    public LogReplicationTask scheduleLogReplicationTask(@Nonnull Runnable task) {
        log.debug("schedule log replication task");
        return LogReplicationTask.NONE;
    }

    @Nonnull
    @Override
    public ElectionTimeout scheduleElectionTimeout(@Nonnull Runnable task) {
        log.debug("schedule election timeout");
        return ElectionTimeout.NONE;
    }

    @Override
    public void stop() throws InterruptedException {

    }
}
