package com.daken.raft.core.rpc.message;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;

/**
 * AppendEntriesRpcMessage
 */
public class AppendEntriesRpcMessage extends AbstractRpcMessage<AppendEntriesRpc> {

    public AppendEntriesRpcMessage(AppendEntriesRpc rpc, NodeId sourceNodeId, Channel channel) {
        super(rpc, sourceNodeId, channel);
    }
}
