package com.daken.raft.core.log.snapshot.entity;

import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.log.snapshot.SnapshotChunk;
import com.daken.raft.core.node.NodeEndpoint;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * 空日志快照，主要在刚开始没有日志快照的时候使用，类似于 null object
 */
public class EmptySnapshot implements Snapshot {

    @Override
    public int getLastIncludedIndex() {
        return 0;
    }

    @Override
    public int getLastIncludedTerm() {
        return 0;
    }

    @Nonnull
    @Override
    public Set<NodeEndpoint> getLastConfig() {
        return Collections.emptySet();
    }

    @Override
    public long getDataSize() {
        return 0;
    }

    /**
     * 只支持 offset == 0 的情况
     * @param offset
     * @param length
     * @return
     */
    @Nonnull
    @Override
    public SnapshotChunk readData(int offset, int length) {
        if (offset == 0) {
            return new SnapshotChunk(new byte[0], true);
        }
        throw new IllegalArgumentException("offset > 0");
    }

    @Nonnull
    @Override
    public InputStream getDataStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void close() {

    }
}
