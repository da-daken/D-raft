package com.daken.raft.core.rpc.message;

import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import lombok.Getter;

/**
 * AppendEntriesResultMessage
 */
@Getter
public class AppendEntriesResultMessage extends AbstractRpcMessage<AppendEntriesResult> {

    private final AppendEntriesRpc appendEntriesRpc;

    public AppendEntriesResultMessage(AppendEntriesResult rpc, NodeId sourceNodeId, Channel channel, AppendEntriesRpc appendEntriesRpc) {
        super(rpc, sourceNodeId, channel);
        this.appendEntriesRpc = appendEntriesRpc;
    }
}
