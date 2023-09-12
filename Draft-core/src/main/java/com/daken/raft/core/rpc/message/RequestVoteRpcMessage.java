package com.daken.raft.core.rpc.message;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;

/**
 * RequestVoteRpcMessage
 */
public class RequestVoteRpcMessage extends AbstractRpcMessage<RequestVoteRpc> {

    public RequestVoteRpcMessage(RequestVoteRpc rpc, NodeId sourceNodeId, Channel channel) {
        super(rpc, sourceNodeId, channel);
    }
}
