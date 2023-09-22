package com.daken.raft.core.rpc.message;

import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.daken.raft.core.rpc.message.resp.InstallSnapshotResult;
import com.google.common.base.Preconditions;
import com.daken.raft.core.node.NodeId;

import javax.annotation.Nonnull;

/**
 * InstallSnapshotResultMessage
 */
public class InstallSnapshotResultMessage {

    private final InstallSnapshotResult result;
    private final NodeId sourceNodeId;
    private final InstallSnapshotRpc rpc;

    public InstallSnapshotResultMessage(InstallSnapshotResult result, NodeId sourceNodeId, @Nonnull InstallSnapshotRpc rpc) {
        Preconditions.checkNotNull(rpc);
        this.result = result;
        this.sourceNodeId = sourceNodeId;
        this.rpc = rpc;
    }

    public InstallSnapshotResult get() {
        return result;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public InstallSnapshotRpc getRpc() {
        return rpc;
    }

}
