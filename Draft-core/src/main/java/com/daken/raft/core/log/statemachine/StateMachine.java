package com.daken.raft.core.log.statemachine;

import com.daken.raft.core.log.entry.Entry;

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
    void applyLog(StateMachineContext context, int index, byte[] commandBytes, int firstLogIndex);

    void shutdown();
}
