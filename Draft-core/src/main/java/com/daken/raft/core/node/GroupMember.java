package com.daken.raft.core.node;

import lombok.Getter;

import java.util.Objects;

/**
 * State of group member
 */
@Getter
public class GroupMember {

    private final NodeEndpoint endpoint;
    // 复制进度
    private ReplicatingState replicatingState;

    public GroupMember(NodeEndpoint endpoint) {
        Objects.requireNonNull(endpoint);
        this.endpoint = endpoint;
    }

    public GroupMember(NodeEndpoint endpoint, ReplicatingState replicatingState) {
        this.endpoint = endpoint;
        this.replicatingState = replicatingState;
    }

    NodeId getId() {
        return endpoint.getId();
    }

    boolean idEquals(NodeId id) {
        return endpoint.getId().equals(id);
    }

    void setReplicatingState(ReplicatingState replicatingState) {
        this.replicatingState = replicatingState;
    }

    boolean isReplicationStateSet() {
        return replicatingState != null;
    }

    private ReplicatingState ensureReplicatingState() {
        if (replicatingState == null) {
            throw new IllegalStateException("replication state not set");
        }
        return replicatingState;
    }

    int getNextIndex() {
        return ensureReplicatingState().getNextIndex();
    }

    int getMatchIndex() {
        return ensureReplicatingState().getMatchIndex();
    }

    public boolean advanceReplicatingState(int lastEntryIndex) {
        return ensureReplicatingState().advance(lastEntryIndex);
    }

    public boolean backOfNextIndex() {
        return ensureReplicatingState().backOffNextIndex();
    }
}
