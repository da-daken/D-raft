package com.daken.raft.core.log.snapshot.entity.builder;


import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;

/**
 * 对日志快照的写入接口，为了处理不同类型的日志快照，使用泛型
 * @param <T>
 */
public interface SnapshotBuilder<T extends Snapshot> {

    /**
     * 追加日志快照
     */
    void append(InstallSnapshotRpc rpc);

    /**
     * 导出日志快照
     */
    T build();

    /**
     * 关闭日志快照(清理)
     */
    void close();

}
