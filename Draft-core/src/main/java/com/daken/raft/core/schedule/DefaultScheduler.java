package com.daken.raft.core.schedule;

import com.daken.raft.core.node.config.NodeConfig;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * DefaultScheduler
 */
@Slf4j
public class DefaultScheduler implements Scheduler {

    /**
     * 最小选举超时时间
     */
    private final int minElectionTimeout;

    /**
     * 最大选举超时时间
     */
    private final int maxElectionTimeout;

    /**
     * 日志复制初始化延迟时间
     */
    private final int logReplicationDelay;

    /**
     * 日志复制间隔
     */
    private final int logReplicationInterval;

    /**
     * 随机
     */
    private final Random electionTimeoutRandom;

    /**
     * 延迟执行线程池
     */
    private final ScheduledExecutorService scheduledExecutorService;

    public DefaultScheduler(NodeConfig config) {
        this(config.getMinElectionTimeout(), config.getMaxElectionTimeout(),
                config.getLogReplicationDelay(), config.getLogReplicationInterval());
    }

    public DefaultScheduler(int minElectionTimeout, int maxElectionTimeout, int logReplicationDelay, int logReplicationInterval) {
        if (minElectionTimeout <= 0 || maxElectionTimeout <= 0 || minElectionTimeout > maxElectionTimeout) {
            throw new IllegalArgumentException("election timeout should not be 0 or min > max");
        }
        if (logReplicationDelay < 0 || logReplicationInterval <= 0) {
            throw new IllegalArgumentException("log replication delay < 0 or log replication interval <= 0");
        }
        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.logReplicationDelay = logReplicationDelay;
        this.logReplicationInterval = logReplicationInterval;
        this.electionTimeoutRandom = new Random();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "scheduler"));
    }

    @Nonnull
    @Override
    public LogReplicationTask scheduleLogReplicationTask(@Nonnull Runnable task) {
        Objects.requireNonNull(task);
        log.debug("submit log replication task");
        ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(
                task, logReplicationDelay, logReplicationInterval, TimeUnit.MILLISECONDS);
        return new LogReplicationTask(scheduledFuture);
    }

    @Nonnull
    @Override
    public ElectionTimeout scheduleElectionTimeout(@Nonnull Runnable task) {
        Objects.requireNonNull(task);
        log.debug("submit election timeout task");
        int timeout = electionTimeoutRandom.nextInt(maxElectionTimeout - minElectionTimeout) + minElectionTimeout;
        ScheduledFuture<?> future = this.scheduledExecutorService.schedule(task, timeout, TimeUnit.MILLISECONDS);
        return new ElectionTimeout(future);
    }

    @Override
    public void stop() throws InterruptedException {
        log.info("start top default scheduler");
        this.scheduledExecutorService.shutdown();
        this.scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
        log.info("top default scheduler success");
    }
}
