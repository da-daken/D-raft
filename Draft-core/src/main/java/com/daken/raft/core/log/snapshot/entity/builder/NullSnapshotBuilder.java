package com.daken.raft.core.log.snapshot.entity.builder;


import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;

/**
 * NullSnapshotBuilder
 */
public class NullSnapshotBuilder implements SnapshotBuilder {

    @Override
    public void append(InstallSnapshotRpc rpc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Snapshot build() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }
}
