package com.daken.raft.core.log.snapshot.entity;

import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.log.snapshot.SnapshotChunk;
import com.daken.raft.core.node.NodeEndpoint;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * 基于内存的日志快照
 */
@Slf4j
@ToString
public class MemorySnapshot implements Snapshot {

    private final int lastIncludedIndex;
    private final int lastIncludedTerm;
    private final byte[] data;
    private final Set<NodeEndpoint> lastConfig;

    public MemorySnapshot(int lastIncludedIndex, int lastIncludedTerm) {
        this(lastIncludedIndex, lastIncludedTerm, new byte[0], Collections.emptySet());
    }

    public MemorySnapshot(int lastIncludedIndex, int lastIncludedTerm, byte[] data, Set<NodeEndpoint> lastConfig) {
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.lastConfig = lastConfig;
    }

    @Override
    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    @Override
    public int getLastIncludedTerm() {
        return lastIncludedTerm;
    }

    @Nonnull
    @Override
    public Set<NodeEndpoint> getLastConfig() {
        return lastConfig;
    }

    @Override
    public long getDataSize() {
        return data.length;
    }

    @Nonnull
    @Override
    public SnapshotChunk readData(int offset, int length) {
        if (offset < 0 || offset > data.length) {
            throw new IndexOutOfBoundsException("offset " + offset + " out of bound");
        }
        // 若剩余内容比要取得的长度小，取剩余内容即可
        // 若剩余内容比要取得的长度大，取要取得的长度
        int bufferLength = Math.min(data.length - offset, length);
        byte[] bytes = new byte[bufferLength];
        System.arraycopy(data, offset, bytes, 0, bufferLength);
        return new SnapshotChunk(bytes, offset + length >= data.length);
    }

    @Nonnull
    @Override
    public InputStream getDataStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public void close() {

    }
}
