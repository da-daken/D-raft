package com.daken.raft.core.rpc.message;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;

import javax.annotation.Nullable;

/**
 * InstallSnapshotRpcMessage
 */
public class InstallSnapshotRpcMessage extends AbstractRpcMessage<InstallSnapshotRpc> {

    public InstallSnapshotRpcMessage(InstallSnapshotRpc rpc, NodeId sourceNodeId, @Nullable Channel channel) {
        super(rpc, sourceNodeId, channel);
    }
}
