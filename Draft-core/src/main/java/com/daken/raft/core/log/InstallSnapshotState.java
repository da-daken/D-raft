package com.daken.raft.core.log;

import com.daken.raft.core.node.NodeEndpoint;

import java.util.Set;

/**
 * 日志快照下载状态
 */
public class InstallSnapshotState {

    public enum StateName {
        ILLEGAL_INSTALL_SNAPSHOT_RPC,
        INSTALLING,
        INSTALLED
    }

    private final StateName stateName;
    private Set<NodeEndpoint> lastConfig;

    public InstallSnapshotState(StateName stateName) {
        this.stateName = stateName;
    }

    public InstallSnapshotState(StateName stateName, Set<NodeEndpoint> lastConfig) {
        this.stateName = stateName;
        this.lastConfig = lastConfig;
    }

    public StateName getStateName() {
        return stateName;
    }

    public Set<NodeEndpoint> getLastConfig() {
        return lastConfig;
    }
}
