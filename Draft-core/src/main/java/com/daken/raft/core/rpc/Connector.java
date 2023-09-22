package com.daken.raft.core.rpc;

import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.rpc.message.InstallSnapshotRpcMessage;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.rpc.message.resp.InstallSnapshotResult;
import com.daken.raft.core.rpc.message.resp.RequestVoteResult;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface Connector {

    void initialize();

    void close();

    void resetChannels();

    void sendRequestVote(@Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints);

    void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull NodeEndpoint destinationEndpoint);

    void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint);

    void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull NodeEndpoint destinationEndpoint);

    void sendInstallSnapshot(@Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint);

    void replyInstallSnapshot(@Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage);

}
