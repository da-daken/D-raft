package com.daken.raft.core.log.snapshot;

import com.daken.raft.core.node.NodeEndpoint;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Set;

/**
 * Snapshot 日志快照接口
 */
public interface Snapshot {

    /**
     * 获取最后一条日志的索引
     */
    int getLastIncludedIndex();

    /**
     * 获取最后一条日志的任期
     */
    int getLastIncludedTerm();

    @Nonnull
    Set<NodeEndpoint> getLastConfig();

    /**
     * 获取数据长度
     */
    long getDataSize();

    /**
     * 根据偏移量和长度读取数据块
     */
    @Nonnull
    SnapshotChunk readData(int offset, int length);

    /**
     * 获取数据流，不用获取全量数据，避免把数据快照的所有数据加载到内存中
     */
    @Nonnull
    InputStream getDataStream();

    /**
     * 关闭文件
     */
    void close();

}
