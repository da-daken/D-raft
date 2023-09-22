package com.daken.raft.core.log.statemachine;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.node.NodeEndpoint;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * 状态机接口（上层服务连入raft集群核心组件的接口）
 */
public interface StateMachine {

    /**
     * 获取 lastApplied
     */
    int getLastApplied();

    /**
     * 应用日志
     */
    void applyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex, Set<NodeEndpoint> lastGroupConfig);

    void shutdown();

    /**
     * 是否生成快照
     *
     * @param firstLogIndex first log index in log files, may not be {@code 0}
     * @param lastApplied   last applied log index
     * @return true if should generate, otherwise false
     */
    boolean shouldGenerateSnapshot(int firstLogIndex, int lastApplied);

    void applySnapshot(@Nonnull Snapshot snapshot) throws IOException;

    void generateSnapshot(@Nonnull OutputStream output) throws IOException;
}
