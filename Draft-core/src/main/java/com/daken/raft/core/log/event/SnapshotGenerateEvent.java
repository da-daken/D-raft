package com.daken.raft.core.log.event;

/**
 * SnapshotGenerateEvent
 */
public class SnapshotGenerateEvent {

    private final int lastIncludedIndex;

    public SnapshotGenerateEvent(int lastIncludedIndex) {
        this.lastIncludedIndex = lastIncludedIndex;
    }

    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }
}
