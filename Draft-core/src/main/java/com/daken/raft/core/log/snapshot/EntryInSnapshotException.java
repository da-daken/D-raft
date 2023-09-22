package com.daken.raft.core.log.snapshot;

import com.daken.raft.core.log.LogException;

/**
 * EntryInSnapshotException
 */
public class EntryInSnapshotException extends LogException {

    private final int index;

    public EntryInSnapshotException(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
