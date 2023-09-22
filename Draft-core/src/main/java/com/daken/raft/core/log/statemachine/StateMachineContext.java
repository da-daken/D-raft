package com.daken.raft.core.log.statemachine;

import com.daken.raft.core.node.NodeEndpoint;

import java.io.OutputStream;
import java.util.Set;

/**
 * StateMachineContext
 */
public interface StateMachineContext {


    /**
     * 生成日志快照
     */
    @Deprecated
    void generateSnapshot(int lastIncludedIndex);


}
