package com.daken.raft.core.node.config;

import com.daken.raft.core.log.Log;
import lombok.Getter;
import lombok.Setter;

/**
 * NodeConfig
 */
@Getter
@Setter
public class NodeConfig {

    /**
     * Minimum election timeout
     */
    private int minElectionTimeout = 3000;

    /**
     * Maximum election timeout
     */
    private int maxElectionTimeout = 4000;

    /**
     * Delay for first log replication after becoming leader
     */
    private int logReplicationDelay = 0;

    /**
     * Interval for log replication task.
     * More specifically, interval for heartbeat rpc.
     * Append entries rpc may be sent less than this interval.
     * e.g after receiving append entries result from followers.
     */
    private int logReplicationInterval = 1000;

    /**
     * Read timeout to receive response from follower.
     * If no response received from follower, resend log replication rpc.
     */
    private int logReplicationReadTimeout = 900;

    /**
     * Max entries to send when replicate log to followers
     */
    private int maxReplicationEntries = Log.ALL_ENTRIES;

    /**
     * Max entries to send when replicate log to new node
     */
    private int maxReplicationEntriesForNewNode = Log.ALL_ENTRIES;

    /**
     * Data length in install snapshot rpc.
     */
    private int snapshotDataLength = 1024;

    /**
     * Worker thread count in nio connector.
     */
    private int nioWorkerThreads = 0;

    /**
     * Max round for new node to catch up.
     */
    private int newNodeMaxRound = 10;

    /**
     * Read timeout to receive response from new node.
     * Default to election timeout.
     */
    private int newNodeReadTimeout = 3000;

    /**
     * Timeout for new node to make progress.
     * If new node cannot make progress after this timeout, new node cannot be added and reply TIMEOUT.
     * Default to election timeout
     */
    private int newNodeAdvanceTimeout = 3000;

    /**
     * Timeout to wait for previous group config change to complete.
     * Default is {@code 0}, forever.
     */
    private int previousGroupConfigChangeTimeout = 0;

}
