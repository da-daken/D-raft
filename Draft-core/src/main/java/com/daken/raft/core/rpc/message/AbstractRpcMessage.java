package com.daken.raft.core.rpc.message;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import lombok.Getter;

/**
 * AbstractRpcMessage
 */
@Getter
public abstract class AbstractRpcMessage<T> {

    private T rpc;
    private final NodeId sourceNodeId;
    private final Channel channel;

    public AbstractRpcMessage(T rpc, NodeId sourceNodeId, Channel channel) {
        this.rpc = rpc;
        this.sourceNodeId = sourceNodeId;
        this.channel = channel;
    }
}
